package com.example.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统消息数据访问层接口
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}