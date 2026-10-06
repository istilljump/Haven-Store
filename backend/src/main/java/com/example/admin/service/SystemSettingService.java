package com.example.admin.service;

import com.example.admin.dto.SystemSettingDTO;

/**
 * 系统设置读写服务
 * <p>
 * 设置持久化到数据库 system_setting 单行表；业务侧（上传限制、登录锁定、
 * 会话时效、自动下架等）统一从这里读取，保证「管理后台改了设置真的生效」
 *
 * @author ZCode
 * @date 2026/10/05
 */
public interface SystemSettingService {

    /**
     * 读取系统设置（未保存过或数据损坏时返回默认值，保证业务可继续）
     *
     * @return 系统设置（永不为 null）
     */
    SystemSettingDTO get();

    /**
     * 保存系统设置（整体覆盖，upsert 单行）
     *
     * @param dto 系统设置
     */
    void save(SystemSettingDTO dto);

    /**
     * 设置内容是否合法（供管理后台保存前校验，返回错误信息，null 表示合法）
     *
     * @param dto 待保存的设置
     * @return 错误信息；null 表示合法
     */
    String validate(SystemSettingDTO dto);
}
