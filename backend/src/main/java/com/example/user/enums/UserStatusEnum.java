package com.example.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户账号状态枚举
 * <p>
 * 与数据库 user.status 字段对应，系统内禁止使用魔法值判断状态
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    /** 账号正常 */
    ENABLE(1, "正常"),

    /** 账号禁用 */
    DISABLE(0, "禁用");

    /** 状态码 */
    private final Integer code;

    /** 状态描述 */
    private final String desc;
}
