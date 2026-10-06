package com.example.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.report.dto.ReportHandleDTO;
import com.example.report.dto.ReportSubmitDTO;
import com.example.report.entity.Report;
import com.example.report.vo.AdminReportVO;

/**
 * 举报模块业务逻辑接口
 *
 * @author ZCode
 * @date 2026/10/05
 */
public interface ReportService extends IService<Report> {

    /**
     * 提交举报（登录用户）
     * <p>
     * 举报人取登录态；同一用户对同一对象存在「待处理」举报时不重复受理
     *
     * @param dto 举报入参
     */
    void submit(ReportSubmitDTO dto);

    /**
     * 分页查询举报列表（管理后台）
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param status     处理状态（0 待处理，1 已处理，2 已驳回；为空表示不限）
     * @param targetType 举报对象类型（product/comment；为空表示不限）
     * @return 举报分页数据
     */
    Page<AdminReportVO> listForAdmin(Integer page, Integer pageSize, Integer status, String targetType);

    /**
     * 处理举报（管理后台）
     *
     * @param reportId 举报 ID
     * @param dto      处理入参（动作 + 备注）
     * @param adminId  处理人用户 ID
     */
    void handle(Long reportId, ReportHandleDTO dto, Long adminId);
}
