package com.example.product.controller;

import com.example.common.Result;
import com.example.product.service.CreditService;
import com.example.product.vo.CreditVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卖家信用接口（轮4：可信交易轻量层）
 * <p>
 * 完整路径：GET /api/credit/{userId}；信用画像不含敏感字段（成交件数/评分/分数），
 * 对游客开放（放行配置见 application.yml 的 jwt.public-paths）
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Api(tags = "卖家信用接口")
@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
public class CreditController {

    /** 卖家信用服务对象 */
    private final CreditService creditService;

    /**
     * 查询卖家信用画像
     *
     * @param userId 卖家用户 ID
     * @return 信用画像（成交件数/评价均分/有效举报/信用分/等级）
     */
    @ApiOperation(value = "卖家信用画像", notes = "无需登录；确定性公式：基线60 + 成交×3 +（评分-3）×10 - 有效举报×5，夹取[0,100]")
    @GetMapping("/{userId}")
    public Result<CreditVO> credit(@PathVariable Long userId) {
        return Result.success(creditService.getCredit(userId));
    }
}
