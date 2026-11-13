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

    /**
     * 卖家发送议价报价（轮8 业务迭代新增）
     * <p>
     * 校验链：议价总开关 → 发送者是商品发布者 → 双方存在该商品会话 → 商品在售
     * → 报价低于现价。同一会话的旧待处理报价会被置为失效（一次只有一个有效报价）。
     *
     * @param dto 接收人 + 商品 + 报价金额
     * @return 报价消息 ID
     */
    Long sendOffer(com.example.message.dto.SendOfferDTO dto);

    /**
     * 买家接受报价并按议价价创建订单（轮8 业务迭代新增）
     * <p>
     * 校验链：接收者是报价消息的接收人 → 报价待处理且未过期 → 商品仍在售。
     * 接受后调用订单服务按议价价创建订单（成交快照即议价价，回流行情样本），
     * 并把该商品其余待处理报价置为失效。
     *
     * @param messageId 报价消息 ID
     * @return 订单 VO（orderNo 供前端跳转）
     */
    com.example.order.vo.OrderVO acceptOffer(Long messageId);
}
