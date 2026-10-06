package com.example.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置数据访问对象
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSetting> {
}
