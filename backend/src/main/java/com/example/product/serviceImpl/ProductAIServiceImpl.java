package com.example.product.serviceImpl;

import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 商品AI估价服务实现类
 * <p>
 * 基础版本：使用规则引擎进行估价（作为占位符，后续可替换为真实AI模型）
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

    /** 随机数生成器 */
    private final Random random = new Random();

    /** 缓存：商品特征到估价结果的映射 */
    private final Map<String, ProductEstimateVO> estimationCache = new HashMap<>();

    /**
     * 商品估价
     * <p>
     * 基于商品特征进行智能估价
     * 当前版本使用规则引擎（后续可替换为真实AI模型）
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

            // 2. 特征提取（简化版）
            Map<String, Object> features = extractFeatures(dto);

            // 3. 规则引擎估价（模拟AI模型）
            ProductEstimateVO result = ruleBasedEstimation(features);

            log.info("商品估价完成，商品标题：{}，估价：{}元", dto.getTitle(), result.getEstimatedPrice());

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
     */
    private void validateEstimateParams(ProductEstimateDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品标题不能为空");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品描述不能为空");
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
        features.put("description_length", dto.getDescription().length());
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
     * 规则引擎估价（模拟AI模型）
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

        // 添加随机波动（模拟AI模型的不确定性）
        int randomBonus = random.nextInt(400) - 200; // -200 到 +200 的随机值
        int finalPrice = basePrice + conditionBonus + randomBonus;

        // 确保价格不低于100
        finalPrice = Math.max(100, finalPrice);

        ProductEstimateVO result = new ProductEstimateVO();
        result.setEstimateId(System.currentTimeMillis()); // 简单的估价ID生成
        result.setEstimatedPrice(BigDecimal.valueOf(finalPrice));
        result.setPriceRange(String.format("%d-%d", finalPrice - 200, finalPrice + 200));
        result.setConfidence(80 + random.nextInt(20)); // 80-99的置信度
        result.setSuggestion("根据商品特征分析，建议价格区间为" + result.getPriceRange() + "元");
        result.setEstimateTime(LocalDateTime.now());

        return result;
    }
}