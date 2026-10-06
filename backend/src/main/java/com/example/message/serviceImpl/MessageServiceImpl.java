package com.example.message.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.BusinessException;
import com.example.common.ResultCodeEnum;
import com.example.message.entity.Message;
import com.example.message.mapper.MessageMapper;
import com.example.message.service.MessageService;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统消息模块业务逻辑实现类
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public Page<Message> getMessageList(Integer page, Integer size) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getReceiverId, getCurrentUserId())
                   .orderByDesc(Message::getCreateTime);

        Page<Message> pageObj = new Page<>(page, size);
        return messageMapper.selectPage(pageObj, queryWrapper);
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getId, messageId)
                   .eq(Message::getReceiverId, getCurrentUserId());
        
        Message updateMessage = new Message();
        updateMessage.setIsRead(1);
        updateMessage.setUpdateTime(LocalDateTime.now());
        
        messageMapper.update(updateMessage, queryWrapper);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getReceiverId, getCurrentUserId())
                   .eq(Message::getIsRead, 0);
        
        Message updateMessage = new Message();
        updateMessage.setIsRead(1);
        updateMessage.setUpdateTime(LocalDateTime.now());
        
        messageMapper.update(updateMessage, queryWrapper);
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getId, messageId)
                   .eq(Message::getReceiverId, getCurrentUserId());
        
        messageMapper.delete(queryWrapper);
    }

    @Override
    public Integer getUnreadCount() {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getReceiverId, getCurrentUserId())
                   .eq(Message::getIsRead, 0);
        
        return Math.toIntExact(messageMapper.selectCount(queryWrapper));
    }

    /**
     * 获取当前登录用户ID
     * <p>
     * 登录态由 JWT 拦截器写入 UserHolder，这里统一从线程上下文读取。
     * 此前这里写死返回 1L，导致任何登录用户看到的都是 1 号用户的系统消息
     */
    private Long getCurrentUserId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }
}
