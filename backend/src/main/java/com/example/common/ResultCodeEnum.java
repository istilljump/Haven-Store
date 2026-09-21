package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 * <p>
 * 系统内禁止出现魔法值状态码，一律引用本枚举定义
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    /** 成功 */
    SUCCESS(200, "成功"),

    /** 参数校验失败 */
    PARAM_ERROR(400, "参数校验失败"),

    /** 未登录或 Token 已失效 */
    UNAUTHORIZED(401, "未登录或 Token 已失效"),

    /** 无权限访问 */
    FORBIDDEN(403, "无权限访问"),

    /** 业务处理异常 */
    BUSINESS_ERROR(500, "业务处理异常"),

    /** 系统内部错误 */
    SYSTEM_ERROR(501, "系统内部错误");

    /** 响应状态码 */
    private final Integer code;

    /** 状态描述信息 */
    private final String msg;
}
