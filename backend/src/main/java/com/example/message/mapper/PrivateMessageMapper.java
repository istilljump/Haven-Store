package com.example.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.message.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 私信数据访问对象
 * <p>
 * 会话聚合、未读统计等查询通过 MyBatis-Plus 的 QueryWrapper + selectMaps 完成，
 * 暂无需自定义 XML
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
}
