package com.example.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.search.entity.SearchLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索埋点数据访问层
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Mapper
public interface SearchLogMapper extends BaseMapper<SearchLog> {
}
