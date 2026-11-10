package com.example.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.search.entity.SearchCorrection;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索纠错词表数据访问层
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Mapper
public interface SearchCorrectionMapper extends BaseMapper<SearchCorrection> {
}
