package com.example.recommend.service;

import com.example.recommend.vo.RecommendItemVO;

import java.util.List;

/**
 * 场景化规则推荐服务
 * <p>
 * 三个推荐位置、五条校园规则（R1~R5），全部锚定「校园闲置 + 成交行情 + 校区」三要素，
 * 不做全局热门/最新凑数；每条推荐强制携带后端生成的可解释文案。
 * 后台「交易设置」的 recommendEnabled 总控（关闭时接口返回 null，前端隐藏区块）
 *
 * @author ZCode
 * @date 2026/11/11
 */
public interface RecommendService {

    /**
     * 首页冷启动推荐（R1 校区成交活跃 → R2 本校高频品类 → 校区最新在售兜底）
     *
     * @param campusId 校区上下文（可空：空则退化为「校园最新闲置」）
     * @return 推荐列表；总开关关闭时返回 null
     */
    List<RecommendItemVO> homeRecommend(Long campusId);

    /**
     * 搜索后互补闲置推荐（R3：互补品类字典命中才返回，不退化为同类凑数）
     *
     * @param keyword    搜索词
     * @param campusId   校区上下文（可空）
     * @param excludeIds 需要排除的商品（当前搜索结果已出现的）
     * @return 互补推荐列表；总开关关闭时返回 null，未命中字典返回空列表
     */
    List<RecommendItemVO> complementaryRecommend(String keyword, Long campusId, List<Long> excludeIds);

    /**
     * 商品详情页推荐（R4 同校同款三梯队 → R5 互补闲置）
     *
     * @param productId 当前商品 ID
     * @return 推荐列表；总开关关闭或商品不存在时返回 null
     */
    List<RecommendItemVO> productRecommend(Long productId);
}
