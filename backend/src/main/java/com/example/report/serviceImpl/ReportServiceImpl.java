package com.example.report.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.BusinessException;
import com.example.product.entity.Comment;
import com.example.product.entity.Product;
import com.example.product.mapper.CommentMapper;
import com.example.product.mapper.ProductMapper;
import com.example.report.dto.ReportHandleDTO;
import com.example.report.dto.ReportSubmitDTO;
import com.example.report.entity.Report;
import com.example.report.mapper.ReportMapper;
import com.example.report.service.ReportService;
import com.example.report.vo.AdminReportVO;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 举报模块业务逻辑实现类
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    /** 举报状态：待处理 */
    private static final int REPORT_STATUS_PENDING = 0;

    /** 举报状态：已处理 */
    private static final int REPORT_STATUS_HANDLED = 1;

    /** 举报状态：已驳回 */
    private static final int REPORT_STATUS_DISMISSED = 2;

    /** 评论状态：隐藏（与 comment.status 字段对应） */
    private static final int COMMENT_STATUS_HIDDEN = 0;

    private final ReportMapper reportMapper;

    private final UserMapper userMapper;

    private final ProductMapper productMapper;

    private final CommentMapper commentMapper;

    @Override
    public void submit(ReportSubmitDTO dto) {
        Long reporterId = UserHolder.getUserId();
        if (reporterId == null) {
            throw new BusinessException("请先登录后再举报");
        }
        // 校验被举报对象真实存在
        if ("product".equals(dto.getTargetType())) {
            if (productMapper.selectById(dto.getTargetId()) == null) {
                throw new BusinessException("被举报的商品不存在");
            }
        } else {
            if (commentMapper.selectById(dto.getTargetId()) == null) {
                throw new BusinessException("被举报的评论不存在");
            }
        }
        // 同一对象同一举报人存在待处理举报时不重复受理
        Long pendingCount = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, dto.getTargetType())
                .eq(Report::getTargetId, dto.getTargetId())
                .eq(Report::getStatus, REPORT_STATUS_PENDING));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("您已举报过该内容，请等待管理员处理");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        report.setStatus(REPORT_STATUS_PENDING);
        reportMapper.insert(report);
        log.info("用户提交举报，举报人：{}，类型：{}，目标：{}，原因：{}",
                reporterId, dto.getTargetType(), dto.getTargetId(), dto.getReason());
    }

    @Override
    public Page<AdminReportVO> listForAdmin(Integer page, Integer pageSize, Integer status, String targetType) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(Report::getTargetType, targetType);
        }
        // 待处理的排前面，同状态内按时间倒序
        wrapper.orderByAsc(Report::getStatus)
                .orderByDesc(Report::getCreateTime)
                .orderByDesc(Report::getId);
        Page<Report> reportPage = reportMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<Report> records = reportPage.getRecords();

        // 批量补齐举报人名称与被举报对象摘要，避免 N+1
        Map<Long, String> reporterNameMap = loadReporterNames(records);
        Map<Long, String> productTitleMap = loadProductTitles(records);
        Map<Long, String> commentContentMap = loadCommentContents(records);
        Map<Long, Product> productMap = loadProductMap(records);
        Map<Long, Comment> commentMap = loadCommentMap(records);

        Page<AdminReportVO> result = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        result.setRecords(records.stream().map(r -> {
            AdminReportVO vo = new AdminReportVO();
            vo.setId(r.getId());
            vo.setReporterId(r.getReporterId());
            vo.setReporterUsername(reporterNameMap.get(r.getReporterId()));
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            if ("product".equals(r.getTargetType())) {
                vo.setTargetSummary(productTitleMap.get(r.getTargetId()));
                Product product = productMap.get(r.getTargetId());
                vo.setTargetStatus(product == null ? null : product.getStatus());
            } else {
                vo.setTargetSummary(commentContentMap.get(r.getTargetId()));
                Comment comment = commentMap.get(r.getTargetId());
                vo.setTargetStatus(comment == null ? null : comment.getStatus());
            }
            vo.setReason(r.getReason());
            vo.setDescription(r.getDescription());
            vo.setStatus(r.getStatus());
            vo.setHandleAction(r.getHandleAction());
            vo.setHandleNote(r.getHandleNote());
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long reportId, ReportHandleDTO dto, Long adminId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        if (report.getStatus() != null && report.getStatus() != REPORT_STATUS_PENDING) {
            throw new BusinessException("该举报已处理过，请勿重复处理");
        }

        String action = dto.getAction();
        if ("takeDownProduct".equals(action)) {
            if (!"product".equals(report.getTargetType())) {
                throw new BusinessException("只有商品举报才能执行下架动作");
            }
            Product product = productMapper.selectById(report.getTargetId());
            if (product != null) {
                Product update = new Product();
                update.setId(product.getId());
                update.setStatus(com.example.product.enums.ProductStatusEnum.OFF_SHELF.getCode());
                productMapper.updateById(update);
            }
        } else if ("hideComment".equals(action)) {
            if (!"comment".equals(report.getTargetType())) {
                throw new BusinessException("只有评论举报才能执行隐藏动作");
            }
            Comment comment = commentMapper.selectById(report.getTargetId());
            if (comment != null) {
                Comment update = new Comment();
                update.setId(comment.getId());
                update.setStatus(COMMENT_STATUS_HIDDEN);
                commentMapper.updateById(update);
            }
        }
        // dismiss：仅标记驳回，不对被举报对象做任何处理

        Report update = new Report();
        update.setId(report.getId());
        update.setStatus("dismiss".equals(action) ? REPORT_STATUS_DISMISSED : REPORT_STATUS_HANDLED);
        update.setHandleAction(action);
        update.setHandleNote(StringUtils.hasText(dto.getNote()) ? dto.getNote().trim() : null);
        update.setHandleAdminId(adminId);
        reportMapper.updateById(update);
        log.info("管理员处理举报，举报ID：{}，动作：{}，管理员：{}", reportId, action, adminId);
    }

    /**
     * 批量回查举报人名称
     */
    private Map<Long, String> loadReporterNames(List<Report> reports) {
        List<Long> ids = reports.stream().map(Report::getReporterId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u ->
                        StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername(), (a, b) -> a));
    }

    /**
     * 批量回查被举报商品标题
     */
    private Map<Long, String> loadProductTitles(List<Report> reports) {
        List<Long> ids = reports.stream()
                .filter(r -> "product".equals(r.getTargetType()))
                .map(Report::getTargetId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, Product::getTitle, (a, b) -> a));
    }

    /**
     * 批量回查被举报商品实体
     */
    private Map<Long, Product> loadProductMap(List<Report> reports) {
        List<Long> ids = reports.stream()
                .filter(r -> "product".equals(r.getTargetType()))
                .map(Report::getTargetId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
    }

    /**
     * 批量回查被举报评论内容
     */
    private Map<Long, String> loadCommentContents(List<Report> reports) {
        List<Long> ids = reports.stream()
                .filter(r -> "comment".equals(r.getTargetType()))
                .map(Report::getTargetId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Comment::getId, c ->
                        c.getContent() != null && c.getContent().length() > 50
                                ? c.getContent().substring(0, 50) + "…" : c.getContent(), (a, b) -> a));
    }

    /**
     * 批量回查被举报评论实体
     */
    private Map<Long, Comment> loadCommentMap(List<Report> reports) {
        List<Long> ids = reports.stream()
                .filter(r -> "comment".equals(r.getTargetType()))
                .map(Report::getTargetId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Comment::getId, c -> c, (a, b) -> a));
    }
}
