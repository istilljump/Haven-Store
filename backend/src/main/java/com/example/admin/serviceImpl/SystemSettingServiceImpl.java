package com.example.admin.serviceImpl;

import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.entity.SystemSetting;
import com.example.admin.mapper.SystemSettingMapper;
import com.example.admin.service.SystemSettingService;
import com.example.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统设置读写服务实现
 * <p>
 * 存储介质为数据库 system_setting 单行表（id=1，整体 JSON 覆盖）。
 * 读取失败一律回落默认值——设置属于「尽力而为」的增强配置，
 * 不应因读取异常阻断登录、上传等主流程
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    /** 设置表固定主键：系统只有一份设置 */
    private static final long SETTINGS_ROW_ID = 1L;

    private final SystemSettingMapper systemSettingMapper;

    private final ObjectMapper objectMapper;

    @Override
    public SystemSettingDTO get() {
        try {
            SystemSetting row = systemSettingMapper.selectById(SETTINGS_ROW_ID);
            if (row == null || !StringUtils.hasText(row.getConfigJson())) {
                return SystemSettingDTO.defaults();
            }
            SystemSettingDTO dto = objectMapper.readValue(row.getConfigJson(), SystemSettingDTO.class);
            return dto == null ? SystemSettingDTO.defaults() : dto;
        } catch (Exception e) {
            log.warn("读取系统设置失败，使用默认设置：{}", e.getMessage());
            return SystemSettingDTO.defaults();
        }
    }

    @Override
    public void save(SystemSettingDTO dto) {
        if (dto == null) {
            throw new BusinessException("设置内容不能为空");
        }
        String error = validate(dto);
        if (error != null) {
            throw new BusinessException(error);
        }
        try {
            String json = objectMapper.writeValueAsString(dto);
            SystemSetting row = new SystemSetting();
            row.setId(SETTINGS_ROW_ID);
            row.setConfigJson(json);
            row.setUpdateTime(LocalDateTime.now());
            // 单行 upsert：存在则覆盖，不存在则插入
            if (systemSettingMapper.selectById(SETTINGS_ROW_ID) == null) {
                systemSettingMapper.insert(row);
            } else {
                systemSettingMapper.updateById(row);
            }
            log.info("系统设置已保存，站点名称：{}", dto.getSite().getName());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("系统设置保存失败", e);
            throw new BusinessException("系统设置保存失败，请稍后重试");
        }
    }

    @Override
    public String validate(SystemSettingDTO dto) {
        if (dto == null) {
            return "设置内容不能为空";
        }
        if (dto.getSite() == null || !StringUtils.hasText(dto.getSite().getName())) {
            return "站点名称不能为空";
        }
        if (dto.getUpload() != null) {
            Integer maxFileSize = dto.getUpload().getMaxFileSize();
            if (maxFileSize != null && (maxFileSize < 1 || maxFileSize > 5120)) {
                return "单文件大小上限须在 1-5120 KB 之间（受服务端 5MB 硬上限约束）";
            }
            Integer maxImages = dto.getUpload().getMaxImages();
            if (maxImages != null && (maxImages < 1 || maxImages > 9)) {
                return "单次上传张数须在 1-9 之间";
            }
        }
        if (dto.getTrade() != null) {
            Integer maxPrice = dto.getTrade().getMaxPrice();
            if (maxPrice != null && maxPrice < 0) {
                return "商品价格上限不能为负数（0 表示不限制）";
            }
            Integer autoOfflineHours = dto.getTrade().getAutoOfflineHours();
            if (autoOfflineHours != null && (autoOfflineHours < 0 || autoOfflineHours > 24 * 365)) {
                return "自动下架时长须在 0-8760 小时之间（0 表示不自动下架）";
            }
        }
        if (dto.getSecurity() != null) {
            Integer loginAttempts = dto.getSecurity().getLoginAttempts();
            if (loginAttempts != null && (loginAttempts < 1 || loginAttempts > 20)) {
                return "登录失败次数上限须在 1-20 之间";
            }
            Integer lockDuration = dto.getSecurity().getLockDuration();
            if (lockDuration != null && (lockDuration < 1 || lockDuration > 1440)) {
                return "账号锁定时长须在 1-1440 分钟之间";
            }
            Integer sessionTimeout = dto.getSecurity().getSessionTimeout();
            if (sessionTimeout != null && (sessionTimeout < 5 || sessionTimeout > 60 * 24 * 30)) {
                return "会话超时时间须在 5-43200 分钟之间";
            }
            String strength = dto.getSecurity().getPasswordStrength();
            if (StringUtils.hasText(strength) && !"low".equals(strength) && !"medium".equals(strength) && !"high".equals(strength)) {
                return "密码强度仅支持 low / medium / high";
            }
        }
        return null;
    }
}
