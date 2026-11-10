package com.example.market.controller;

import com.example.common.Result;
import com.example.market.service.MarketDataService;
import com.example.market.vo.MarketBasisVO;
import com.example.market.vo.MarketCategoryStatVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成交行情模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如：GET /api/market/summary；
 * 行情数据不涉及任何用户隐私（只有价格与样本量），接口对游客开放
 * （放行配置见 application.yml 的 jwt.public-paths）
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Api(tags = "成交行情接口")
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    /** 成交行情服务对象 */
    private final MarketDataService marketDataService;

    /**
     * 查询「分类 + 成色」的成交行情基线
     *
     * @param categoryId 分类 ID
     * @param condition  成色
     * @return 行情基线；窗口内无成交时 data 为 null
     */
    @ApiOperation(value = "查询成交行情", notes = "无需登录；返回近 90 天同类成交的样本量、区间与中位价，无样本时 data 为 null")
    @GetMapping("/summary")
    public Result<MarketBasisVO> summary(@RequestParam Integer categoryId,
                                         @RequestParam String condition) {
        return Result.success(marketDataService.getBasis(categoryId, condition));
    }

    /**
     * 校区品类行情 TOP N（首页「本校行情速览」卡片）
     *
     * @param campusId 校区 ID
     * @param topN     取前 N 个品类（默认 3）
     * @return 品类行情列表（按成交件数倒序）
     */
    @ApiOperation(value = "校区品类行情TOP", notes = "无需登录；返回本校近 90 天成交件数最多的品类及其中位价，用于行情看板")
    @GetMapping("/campus-summary")
    public Result<List<MarketCategoryStatVO>> campusSummary(
            @RequestParam Long campusId,
            @RequestParam(defaultValue = "3") Integer topN) {
        return Result.success(marketDataService.getCampusTopCategories(campusId, topN));
    }
}
