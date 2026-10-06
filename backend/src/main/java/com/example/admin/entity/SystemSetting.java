package com.example.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统设置实体（单行表，id 固定为 1）
 * <p>
 * 设置整体以 JSON 文本形式存储在 config_json 字段，
 * 结构与管理后台「系统设置」表单的 {@code SystemSettingDTO} 一一对应；
 * 相比此前的 Redis 存储，落库后设置在 Redis 清空/重启后依然保留
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@TableName("system_setting")
public class SystemSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，固定为 1（系统只有一份设置） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 设置 JSON 文本（SystemSettingDTO 序列化结果） */
    private String configJson;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
