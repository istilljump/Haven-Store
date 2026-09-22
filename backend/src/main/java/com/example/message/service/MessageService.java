package com.example.message.service;

import com.example.message.dto.SendMessageDTO;
import com.example.message.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 系统消息模块业务逻辑接口
 *
 * @author ZCode
 * @date 2026/09/22
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送系统消息
     *
     * @param dto 消息入参
     * @return 消息ID
     */
    Long sendMessage(SendMessageDTO dto);

    /**
     * 获取当前用户的系统消息列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> getMessageList(Integer page, Integer size);

    /**
     * 标记消息为已读
     *
     * @param messageId 消息ID
     */
    void markAsRead(Long messageId);

    /**
     * 标记所有消息为已读
     */
    void markAllAsRead();

    /**
     * 删除消息
     *
     * @param messageId 消息ID
     */
    void deleteMessage(Long messageId);

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数量
     */
    Integer getUnreadCount();
}