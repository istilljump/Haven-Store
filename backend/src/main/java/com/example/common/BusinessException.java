package com.example.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * 业务逻辑中所有可预期的异常均抛出本异常，
 * 由全局异常处理器统一捕获并转换为 Result 格式响应
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误状态码 */
    private final Integer code;

    /**
     * 使用状态码枚举构造业务异常
     *
     * @param resultCodeEnum 状态码枚举
     */
    public BusinessException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMsg());
        this.code = resultCodeEnum.getCode();
    }

    /**
     * 使用自定义提示信息构造业务异常（默认使用业务异常状态码 500）
     *
     * @param message 错误提示信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCodeEnum.BUSINESS_ERROR.getCode();
    }
}
