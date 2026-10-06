package com.example.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.report.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报数据访问对象
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
