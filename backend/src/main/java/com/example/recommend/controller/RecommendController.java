package com.example.recommend.controller;

import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.common.Result;
import com.example.recommend.service.RecommendService;
import com.example.recommend.vo.RecommendItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 场景化推荐模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如：GET /api/recommend/home；
 * 推荐对游客开放（放行配置见 application.yml 的 jwt.public-paths）；
 * 每个条目强制携带可解释 reason，总开关关闭时三个接口统一返回 data=null
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Api(tags = "场景化推荐接口")
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    /** 场景化推荐服务对象 */
    private final RecommendService recommendService;

    /** 系统设置服务（推荐总开关的前置门卫：必须在缓存读取之前拦截，否则缓存会绕过开关语义） */
    private final SystemSettingService systemSettingService;

    /**
     * 推荐总开关（后台「交易设置」的 recommendEnabled，缺省开启）；
     * 与服务层门卫构成双保险——控制器层先行拦截，保证关闭后立即生效
     */
    private boolean recommendEnabled() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() == null || !Boolean.FALSE.equals(settings.getTrade().getRecommendEnabled());
    }

    /**
     * 首页冷启动推荐
     *
     * @param campusId 校区上下文（可空，空则退化为「校园最新闲置」）
     * @return 推荐列表（R1→R2→兜底）
     */
    @ApiOperation(value = "首页冷启动推荐", notes = "无需登录；R1 校区成交活跃 → R2 本校高频品类 → 本校/校园最新闲置兜底，条目带推荐原因")
    @GetMapping("/home")
    public Result<List<RecommendItemVO>> home(@RequestParam(required = false) Long campusId) {
        if (!recommendEnabled()) {
            return Result.success(null);
        }
        return Result.success(recommendService.homeRecommend(campusId));
    }

    /**
     * 搜索后互补闲置推荐
     *
     * @param keyword    搜索词
     * @param campusId   校区上下文（可空）
     * @param excludeIds 需要排除的商品 ID（当前搜索结果已出现的，逗号分隔）
     * @return 互补推荐列表（字典未命中返回空列表，不凑数）
     */
    @ApiOperation(value = "搜索后互补推荐", notes = "无需登录；互补品类字典命中才返回（搜自行车→车锁/头盔），条目带推荐原因")
    @GetMapping("/complementary")
    public Result<List<RecommendItemVO>> complementary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) List<Long> excludeIds) {
        if (!recommendEnabled()) {
            return Result.success(null);
        }
        return Result.success(recommendService.complementaryRecommend(keyword, campusId, excludeIds));
    }

    /**
     * 商品详情页推荐
     *
     * @param productId 当前商品 ID
     * @return 推荐列表（R4 同校同款 → R5 互补）
     */
    @ApiOperation(value = "详情页推荐", notes = "无需登录；R4 同校区+同品类+同成色三梯队 → R5 互补闲置，条目带推荐原因")
    @GetMapping("/for-product/{productId}")
    public Result<List<RecommendItemVO>> forProduct(@PathVariable Long productId) {
        if (!recommendEnabled()) {
            return Result.success(null);
        }
        return Result.success(recommendService.productRecommend(productId));
    }
}
