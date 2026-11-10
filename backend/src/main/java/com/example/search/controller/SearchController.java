package com.example.search.controller;

import com.example.common.Result;
import com.example.search.service.SearchService;
import com.example.search.vo.SearchLogVO;
import com.example.search.vo.SearchPageVO;
import com.example.search.vo.SearchSuggestResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 增强搜索模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如：GET /api/search/suggest；
 * 搜索与补全对游客开放（放行配置见 application.yml 的 jwt.public-paths）。
 * 原 /product/search 接口原样保留，本模块是其配套增强层
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Api(tags = "增强搜索接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    /** 增强搜索服务对象 */
    private final SearchService searchService;

    /**
     * 搜索实时补全
     *
     * @param q        输入串
     * @param campusId 校区上下文（可空）
     * @param limit    候选上限（默认 8）
     * @return 补全候选 + 纠错建议
     */
    @ApiOperation(value = "搜索补全", notes = "无需登录；词库前缀/拼音/本校热搜三级候选，附品类标签与「本校近期成交N件」热提示")
    @GetMapping("/suggest")
    public Result<SearchSuggestResultVO> suggest(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long campusId,
            @RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(searchService.suggest(q, campusId, limit));
    }

    /**
     * 增强搜索
     *
     * @param keyword          关键词
     * @param campusId         校区筛选（可空）
     * @param productCondition 成色二次筛选（仅后台开关开启时生效）
     * @param categoryId       分类筛选（可空）
     * @param minPrice         价格下限（可空）
     * @param maxPrice         价格上限（可空）
     * @param sort             排序方式（可空）
     * @param page             页码
     * @param pageSize         每页条数
     * @return 纠错信息 + 分页（条目含行情标签）+ 埋点 ID
     */
    @ApiOperation(value = "增强搜索", notes = "无需登录；自动纠错、结果内嵌行情标签（品类中位价/价格分位）、写入搜索埋点")
    @GetMapping("/products")
    public Result<SearchPageVO> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String productCondition,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(searchService.search(keyword, campusId, productCondition, categoryId,
                minPrice, maxPrice, sort, page, pageSize));
    }

    /**
     * 点击信标（fire-and-forget）
     *
     * @param body {logId, productId}
     * @return 操作结果
     */
    @ApiOperation(value = "搜索点击信标", notes = "无需登录；把点击的结果商品回填到埋点，供「点击→成交」归因，失败静默")
    @PostMapping("/log/click")
    public Result<Void> click(@RequestBody Map<String, Long> body) {
        searchService.recordClick(body.get("logId"), body.get("productId"));
        return Result.success();
    }

    /**
     * 近期埋点查询（演示/验证用）
     *
     * @param keyword 关键词（可空）
     * @param limit   条数上限（默认 10）
     * @return 埋点列表
     */
    @ApiOperation(value = "近期搜索埋点", notes = "无需登录；演示与验证用，可观察「搜索→点击→成交」归因链路")
    @GetMapping("/log/list")
    public Result<List<SearchLogVO>> logs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(searchService.recentLogs(keyword, limit));
    }
}
