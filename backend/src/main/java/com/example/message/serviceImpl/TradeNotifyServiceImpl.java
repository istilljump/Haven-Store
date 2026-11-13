package com.example.message.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.message.entity.Message;
import com.example.message.service.MessageService;
import com.example.message.service.TradeNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易事件通知服务实现（轮 8 业务迭代新增）
 * <p>
 * 定向写入 system_message：message_type=3（交易消息）、receiver_type=user。
 * 与管理端群发（AdminServiceImpl.sendMessage，带 batch_no）不同，交易通知
 * 为单人/批量定向单发，不带批次号——通知中心按时间线自然混排。
 *
 * @author ZCode
 * @date 2026/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeNotifyServiceImpl implements TradeNotifyService {

    /** 交易消息类型（system_message.message_type=3，此前空壳，本服务为其唯一生产入口） */
    private static final int MESSAGE_TYPE_TRADE = 3;

    /** 定向通知的接收群体标记：交易消息的接收者均为普通用户 */
    private static final String RECEIVER_TYPE_USER = "user";

    private final MessageService messageService;

    @Override
    public void notifyUser(Long userId, String title, String content) {
        if (userId == null) {
            return;
        }
        Message message = buildMessage(userId, title, content);
        messageService.save(message);
        log.info("交易通知：userId={}, title={}", userId, title);
    }

    @Override
    public void notifyUsers(List<Long> userIds, String title, String content) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Message> messages = userIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(uid -> buildMessage(uid, title, content))
                .collect(Collectors.toList());
        if (messages.isEmpty()) {
            return;
        }
        // 批量插入（收藏者可能较多，与管理端群发同款 500 条分批策略）
        messageService.saveBatch(messages, 500);
        log.info("交易批量通知：人数={}, title={}", messages.size(), title);
    }

    @Override
    public boolean hasUnreadNotice(Long userId, String title) {
        if (userId == null) {
            return false;
        }
        return messageService.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getMessageType, MESSAGE_TYPE_TRADE)
                .eq(Message::getTitle, title)
                .eq(Message::getIsRead, 0)) > 0;
    }

    private Message buildMessage(Long userId, String title, String content) {
        Message message = new Message();
        message.setReceiverId(userId);
        message.setMessageType(MESSAGE_TYPE_TRADE);
        message.setReceiverType(RECEIVER_TYPE_USER);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        return message;
    }
}
