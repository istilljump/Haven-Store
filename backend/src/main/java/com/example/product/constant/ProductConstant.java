package com.example.product.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 商品模块常量定义
 * <p>
 * 系统内禁止出现魔法值，商品模块相关常量统一定义在本类
 *
 * @author ZCode
 * @date 2026/09/21
 */
public final class ProductConstant {

    /** 交易方式：线上 */
    public static final String TRADE_TYPE_ONLINE = "线上";

    /** 交易方式：线下 */
    public static final String TRADE_TYPE_OFFLINE = "线下";

    /** 合法交易方式集合（用于业务层校验） */
    public static final Set<String> VALID_TRADE_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(TRADE_TYPE_ONLINE, TRADE_TYPE_OFFLINE)));

    /** 合法成色集合（用于业务层校验） */
    public static final Set<String> VALID_CONDITIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("全新", "九成新", "八成新", "七成新及以下")));

    /** 经度最小值 */
    public static final int LONGITUDE_MIN = -180;

    /** 经度最大值 */
    public static final int LONGITUDE_MAX = 180;

    /** 纬度最小值 */
    public static final int LATITUDE_MIN = -90;

    /** 纬度最大值 */
    public static final int LATITUDE_MAX = 90;

    /**
     * 常量类禁止实例化
     */
    private ProductConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
