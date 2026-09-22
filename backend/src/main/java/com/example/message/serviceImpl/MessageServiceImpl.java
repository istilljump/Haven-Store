package com.example.message.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.message.dto.SendMessageDTO;
import com.example.message.entity.Message;
import com.example.message.mapper.MessageMapper;
import com.example.message.service.MessageService;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
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
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Long sendMessage(SendMessageDTO dto) {
        // 验证接收者是否存在
        User receiver = userMapper.selectById(dto.getReceiverId());
        if (receiver == null) {
            throw new RuntimeException("接收者不存在");
        }

        Message message = new Message();
        message.setReceiverId(dto.getReceiverId());
        message.setMessageType(dto.getMessageType());
        message.setTitle(dto.getTitle());
        message.setContent(dto.getContent());
        message.setIsRead(0); // 未读
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());

        messageMapper.insert(message);
        return message.getId();
    }

    @Override
    public List<Message> getMessageList(Integer page, Integer size) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getReceiverId, getCurrentUserId())
                   .orderByDesc(Message::getCreateTime);
        
        Page<Message> pageObj = new Page<>(page, size);
        Page<Message> result = messageMapper.selectPage(pageObj, queryWrapper);
        
        return result.getRecords();
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
     * TODO: 实现从JWT Token中获取用户ID
     */
    private Long getCurrentUserId() {
        // 这里应该从JWT Token中解析出用户ID
        // 暂时返回一个默认值
        return 1L;
    }
}