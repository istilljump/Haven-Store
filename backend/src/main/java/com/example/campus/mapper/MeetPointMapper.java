package com.example.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campus.entity.MeetPoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面交点模块数据访问层
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Mapper
public interface MeetPointMapper extends BaseMapper<MeetPoint> {
}
