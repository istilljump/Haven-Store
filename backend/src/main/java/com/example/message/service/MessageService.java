package com.example.message.service;

import com.example.message.entity.Message;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统消息模块业务逻辑接口
 * <p>
 * 说明：系统消息的「发送」能力只保留在管理后台（AdminController 群发），
 * 面向普通用户的接口只提供「收件箱」能力（列表/已读/删除/未读数），
 * 避免出现任何登录用户可给任意人伪造系统消息的口子
 *
 * @author ZCode
 * @date 2026/09/22
 */
public interface MessageService extends IService<Message> {

    /**
     * 获取当前用户的系统消息列表（分页）
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    IPage<Message> getMessageList(Integer page, Integer size);

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