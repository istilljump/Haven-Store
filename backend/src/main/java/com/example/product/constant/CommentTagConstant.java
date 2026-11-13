package com.example.product.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 评价预置标签常量（轮8 业务迭代新增）
 * <p>
 * 买家评价时可勾选的正向标签（多选 ≤3 个）：仅允许预置集合内的取值，
 * 避免自由文本标签污染信用聚合；标签为纯正向，评分公式不变。
 *
 * @author ZCode
 * @date 2026/11
 */
public final class CommentTagConstant {

    /** 预置正向标签（顺序即前端展示顺序） */
    public static final List<String> PRESET_TAGS =
            Arrays.asList("准时赴约", "成色如一", "沟通顺畅", "描述相符");

    /** 单条评价最多勾选的标签数 */
    public static final int MAX_TAG_COUNT = 3;

    private CommentTagConstant() {
    }

    /**
     * 校验并规范化标签集合：非空时要求全部命中预置集合且不超过上限
     *
     * @param tags 前端提交的标签
     * @return 校验通过的标签（原样返回，供逗号拼接落库）；空集合返回 null
     */
    public static String validateAndJoin(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        if (tags.size() > MAX_TAG_COUNT) {
            throw new com.example.common.BusinessException("评价标签最多选择 " + MAX_TAG_COUNT + " 个");
        }
        for (String tag : tags) {
            if (!PRESET_TAGS.contains(tag)) {
                throw new com.example.common.BusinessException("评价标签不在可选范围内：" + tag);
            }
        }
        return String.join(",", tags);
    }
}
