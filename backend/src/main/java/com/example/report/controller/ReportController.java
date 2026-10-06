package com.example.report.controller;

import com.example.common.Result;
import com.example.report.dto.ReportSubmitDTO;
import com.example.report.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报模块控制器（用户侧）
 * <p>
 * 提交入口面向登录用户；举报的查询与处理在管理后台接口（/admin/reports/**）
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Api(tags = "举报模块接口")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 提交举报
     *
     * @param dto 举报入参（对象类型、对象 ID、原因、说明）
     * @return 操作结果
     */
    @ApiOperation(value = "提交举报", notes = "需要登录；举报人取登录态，同一对象存在待处理举报时不重复受理")
    @PostMapping
    public Result<Void> submit(@RequestBody @Validated ReportSubmitDTO dto) {
        reportService.submit(dto);
        return Result.success();
    }
}
