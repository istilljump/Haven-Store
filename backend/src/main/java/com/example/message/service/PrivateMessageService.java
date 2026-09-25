package com.example.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.message.dto.SendPrivateMessageDTO;
import com.example.message.entity.PrivateMessage;
import com.example.message.vo.ConversationVO;
import com.example.message.vo.PrivateMessageVO;

import java.util.List;

/**
 * 私信模块业务逻辑接口
 *
 * @author ZCode
 * @date 2026/09/25
 */
public interface PrivateMessageService extends IService<PrivateMessage> {

    /**
     * 发送私信（商品咨询复用本方法，仅多传一个商品 ID）
     *
     * @param dto 发送入参
     * @return 新私信 ID
     */
    Long send(SendPrivateMessageDTO dto);

    /**
     * 查询当前用户的会话列表，按最近一条私信时间倒序
     *
     * @return 会话列表（含对方信息、关联商品、未读条数）
     */
    List<ConversationVO> listConversations();

    /**
     * 查询与某人的聊天记录，并把对方发给我的消息标记为已读
     *
     * @param peerId    对方用户 ID
     * @param productId 关联商品 ID（普通私信传 null）
     * @return 按时间正序的聊天记录
     */
    List<PrivateMessageVO> listChat(Long peerId, Long productId);

    /**
     * 当前用户的未读私信总数
     *
     * @return 未读条数
     */
    Integer getUnreadCount();

    /**
     * 删除与某人的整个会话（双方均不再可见）
     *
     * @param peerId    对方用户 ID
     * @param productId 关联商品 ID（普通私信传 null）
     */
    void deleteConversation(Long peerId, Long productId);
}
