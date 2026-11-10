package com.example.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campus.entity.Campus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 校区模块数据访问层
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Mapper
public interface CampusMapper extends BaseMapper<Campus> {
}
