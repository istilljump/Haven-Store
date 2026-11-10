package com.example.product.serviceImpl;

import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.market.service.MarketDataService;
import com.example.market.vo.MarketBasisVO;
import com.example.product.constant.ProductConstant;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.service.ProductAIService;
import com.example.product.vo.ProductEstimateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品AI估价服务实现类
 * <p>
 * 估价走两条路径，优先级从上到下：
 * 1. <b>成交行情路径（MARKET_DATA）</b>：以平台自身「分类 × 成色」的历史成交为样本，
 *    取中位价作为建议价，依据（样本量/区间/中位价）原样透出——可解释、可断言、可复现；
 * 2. <b>规则基线路径（RULE_BASELINE）</b>：行情样本不足或后台关闭行情估价时，
 *    按标题品牌/型号/成色特征的规则引擎估价，并明示「数据积累中」。
 * <p>
 * 两条路径都是确定性的：同输入必同输出（不再注入随机波动），
 * 「估价是否可信」由冒烟断言与单元测试说话，而不是靠随机数模拟「AI 不确定性」
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAIServiceImpl implements ProductAIService {

    /** 模拟AI模型延迟 */
    private static final int AI_MODEL_DELAY_MS = 500;

    /** 行情估价的最小样本量默认值（后台「交易设置」的 trade.minSampleSize 可覆盖） */
    private static final int DEFAULT_MIN_SAMPLE_SIZE = 3;

    /** 缓存：商品特征到估价结果的映射 */
    private final Map<String, ProductEstimateVO> estimationCache = new HashMap<>();

    /** 成交行情服务（数据驱动定价的数据底座） */
    private final MarketDataService marketDataService;

    /** 系统设置服务：行情估价开关与最小样本量从后台「交易设置」读取 */
    private final SystemSettingService systemSettingService;

    /**
     * 商品估价
     * <p>
     * 优先成交行情，样本不足时降级规则基线；结果整体可缓存
     *
     * @param dto 估价参数
     * @return 估价结果
     */
    @Override
    @Cacheable(value = "productEstimations", key = "#dto.title + #dto.description + #dto.productCondition + #dto.categoryId")
    public Result<ProductEstimateVO> estimatePrice(ProductEstimateDTO dto) {
        try {
            // 1. 参数校验
            validateEstimateParams(dto);

            // 2. 特征提取（规则路径用）
            Map<String, Object> features = extractFeatures(dto);

            // 3. 优先成交行情，样本不足时回退规则引擎
            ProductEstimateVO result = tryMarketBasedEstimation(dto);
            if (result == null) {
                result = ruleBasedEstimation(features);
            }

            log.info("商品估价完成，商品标题：{}，依据：{}，估价：{}元",
                    dto.getTitle(), result.getBasisType(), result.getEstimatedPrice());

            return Result.success(result);
        } catch (BusinessException e) {
            log.warn("商品估价参数错误：{}", e.getMessage());
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("商品估价服务异常", e);
            return Result.fail(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 参数校验
     * <p>
     * 说明：描述允许为空——发布向导第一步就会调用估价，此时用户还没填描述，
     * 估价规则以标题与成色为主
     */
    private void validateEstimateParams(ProductEstimateDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品标题不能为空");
        }
        if (dto.getProductCondition() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品成色不能为空");
        }
        if (!ProductConstant.VALID_CONDITIONS.contains(dto.getProductCondition())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "成色不合法，仅支持：全新、九成新、八成新、七成新及以下");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "分类ID不能为空");
        }
    }

    /**
     * 特征提取（简化版）
     */
    private Map<String, Object> extractFeatures(ProductEstimateDTO dto) {
        Map<String, Object> features = new HashMap<>();
        features.put("title_length", dto.getTitle().length());
        // 描述允许为空（发布向导第一步调用估价时还没有描述）
        features.put("description_length", dto.getDescription() == null ? 0 : dto.getDescription().length());
        features.put("has_brand", dto.getTitle().contains("iPhone") || dto.getTitle().contains("华为") || dto.getTitle().contains("小米"));
        features.put("has_model", dto.getTitle().contains("Pro") || dto.getTitle().contains("Max") || dto.getTitle().contains("Plus"));
        features.put("condition_score", getConditionScore(dto.getProductCondition()));
        features.put("category_id", dto.getCategoryId());
        return features;
    }

    /**
     * 成色评分转换
     */
    private int getConditionScore(String condition) {
        switch (condition) {
            case "全新": return 100;
            case "九成新": return 90;
            case "八成新": return 80;
            case "七成新及以下": return 70;
            default: return 50;
        }
    }

    /**
     * 清除商品估价缓存
     */
    public void clearEstimationCache() {
        estimationCache.clear();
        log.info("商品估价缓存已清除");
    }

    /**
     * 成交行情估价（MARKET_DATA 路径）
     * <p>
     * 建议价 = 同类（分类 × 成色）近 90 天成交的中位价；
     * 样本量不足最小样本阈值或后台关闭行情估价时返回 null，由调用方降级。
     * 任何异常（行情服务不可用等）都不阻断估价，统一回退规则基线
     *
     * @param dto 估价参数
     * @return 行情估价结果；不可用时返回 null
     */
    private ProductEstimateVO tryMarketBasedEstimation(ProductEstimateDTO dto) {
        try {
            // 1. 行情估价开关（后台「交易设置」的 marketBasis，缺省开启）
            SystemSettingDTO settings = systemSettingService.get();
            if (settings != null && settings.getTrade() != null
                    && Boolean.FALSE.equals(settings.getTrade().getMarketBasis())) {
                return null;
            }
            // 2. 最小样本量阈值（后台可调，缺省 3）
            int minSample = settings == null || settings.getTrade() == null || settings.getTrade().getMinSampleSize() == null
                    ? DEFAULT_MIN_SAMPLE_SIZE : settings.getTrade().getMinSampleSize();
            // 3. 取行情基线并校验样本量
            MarketBasisVO basis = marketDataService.getBasis(dto.getCategoryId(), dto.getProductCondition());
            if (basis == null || basis.getSampleSize() == null || basis.getSampleSize() < minSample) {
                return null;
            }
            // 4. 中位价即建议价（确定性：同数据同结果）
            ProductEstimateVO vo = new ProductEstimateVO();
            vo.setEstimateId(System.currentTimeMillis());
            vo.setEstimatedPrice(basis.getMedianPrice());
            String min = basis.getMinPrice().stripTrailingZeros().toPlainString();
            String max = basis.getMaxPrice().stripTrailingZeros().toPlainString();
            vo.setPriceRange(String.format("%s-%s", min, max));
            vo.setConfidence(Math.min(95, 60 + basis.getSampleSize() * 5));
            vo.setSuggestion(String.format("基于近%d天%d件同类成交，建议定价%s元（成交区间%s~%s元）",
                    basis.getDays(), basis.getSampleSize(), vo.getEstimatedPrice().stripTrailingZeros().toPlainString(), min, max));
            vo.setEstimateTime(LocalDateTime.now());
            vo.setBasisType(basis.getBasisType());
            vo.setSampleSize(basis.getSampleSize());
            vo.setMarketMedian(basis.getMedianPrice());
            vo.setMarketMin(basis.getMinPrice());
            vo.setMarketMax(basis.getMaxPrice());
            vo.setReasons(Arrays.asList(
                    String.format("近%d天同类（%s）成交 %d 件", basis.getDays(), dto.getProductCondition(), basis.getSampleSize()),
                    String.format("成交区间 ¥%s ~ ¥%s", min, max),
                    "估价取成交中位价，成交越多估价越准"));
            return vo;
        } catch (Exception e) {
            log.warn("成交行情估价不可用，回退规则基线：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 规则引擎估价（RULE_BASELINE 降级路径）
     * <p>
     * 行情样本不足时的兜底：按特征规则给出确定性建议价，
     * 并在依据中明示「数据积累中」，不假装智能
     */
    private ProductEstimateVO ruleBasedEstimation(Map<String, Object> features) {
        // 模拟AI模型延迟
        try {
            Thread.sleep(AI_MODEL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 基于特征的简单规则估价
        int basePrice = 1000; // 基础价格
        int conditionBonus = (int) features.get("condition_score");
        boolean hasBrand = (boolean) features.get("has_brand");
        boolean hasModel = (boolean) features.get("has_model");
        int titleLength = (int) features.get("title_length");

        // 品牌溢价
        if (hasBrand) {
            basePrice += 500;
        }

        // 型号溢价
        if (hasModel) {
            basePrice += 300;
        }

        // 标题长度调整
        if (titleLength > 20) {
            basePrice += 200;
        }

        // 确定性定价：不再注入随机波动（同输入必同输出，由测试断言兜底）
        int finalPrice = basePrice + conditionBonus;

        // 确保价格不低于100
        finalPrice = Math.max(100, finalPrice);

        ProductEstimateVO result = new ProductEstimateVO();
        result.setEstimateId(System.currentTimeMillis()); // 简单的估价ID生成
        result.setEstimatedPrice(BigDecimal.valueOf(finalPrice));
        result.setPriceRange(String.format("%d-%d", finalPrice - 200, finalPrice + 200));
        result.setConfidence(80); // 规则基线的固定置信度
        result.setSuggestion("根据商品特征分析，建议价格区间为" + result.getPriceRange() + "元");
        result.setEstimateTime(LocalDateTime.now());
        result.setBasisType("RULE_BASELINE");
        result.setReasons(Arrays.asList(
                "平台同类成交还在数据积累中，暂按规则基线估价",
                "规则依据：标题品牌/型号关键词、成色与标题完整度"));
        return result;
    }
}
