package com.example.message.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.BusinessException;
import com.example.common.ResultCodeEnum;
import com.example.message.constant.PrivateMessageConstant;
import com.example.message.dto.SendPrivateMessageDTO;
import com.example.message.entity.PrivateMessage;
import com.example.message.mapper.PrivateMessageMapper;
import com.example.message.service.PrivateMessageService;
import com.example.message.vo.ConversationVO;
import com.example.message.vo.PrivateMessageVO;
import com.example.product.entity.Product;
import com.example.product.mapper.ProductMapper;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 私信模块业务逻辑实现类
 * <p>
 * 会话归属由会话键保证：键由「双方用户 ID 的较小值 + 较大值 + 商品 ID」拼成，
 * 因此只有会话双方能算出同一个键，天然杜绝越权读取他人会话
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage>
        implements PrivateMessageService {

    /** 私信数据访问对象 */
    private final PrivateMessageMapper privateMessageMapper;

    /** 用户数据访问对象（校验接收者、回填对方昵称头像） */
    private final UserMapper userMapper;

    /** 商品数据访问对象（校验被咨询商品、回填商品标题封面） */
    private final ProductMapper productMapper;

    @Override
    public Long send(SendPrivateMessageDTO dto) {
        Long me = requireLoginUserId();
        // 1. 不能给自己发私信（商品详情页对自家商品也会走到这里）
        if (me.equals(dto.getToUserId())) {
            throw new BusinessException("不能给自己发私信");
        }
        // 2. 接收者必须存在
        if (userMapper.selectById(dto.getToUserId()) == null) {
            throw new BusinessException("接收者不存在");
        }
        // 3. 商品咨询需校验商品存在且不是自己发布的
        Long productId = dto.getProductId();
        if (productId != null) {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException("咨询的商品不存在");
            }
            if (me.equals(product.getUserId())) {
                throw new BusinessException("不能咨询自己发布的商品");
            }
        }
        // 4. 内容去掉首尾空白后不允许为空（@NotBlank 拦不住纯空白以外的边界，这里再兜一次）
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("私信内容不能为空");
        }

        PrivateMessage message = new PrivateMessage();
        message.setConversationKey(buildConversationKey(me, dto.getToUserId(), productId));
        message.setFromUserId(me);
        message.setToUserId(dto.getToUserId());
        message.setProductId(productId);
        message.setContent(content);
        message.setIsRead(PrivateMessageConstant.UNREAD);
        privateMessageMapper.insert(message);
        log.info("私信发送成功，私信ID：{}，发送者：{}，接收者：{}，关联商品：{}",
                message.getId(), me, dto.getToUserId(), productId);
        return message.getId();
    }

    @Override
    public List<ConversationVO> listConversations() {
        Long me = requireLoginUserId();
        // 1. 按会话键分组取每组最大 ID（ID 自增，最大值即最近一条），避免把全部私信捞进内存
        QueryWrapper<PrivateMessage> lastMessageWrapper = new QueryWrapper<>();
        lastMessageWrapper.select("conversation_key", "MAX(id) AS last_id")
                .and(wrapper -> wrapper.eq("from_user_id", me).or().eq("to_user_id", me))
                .groupBy("conversation_key");
        List<Map<String, Object>> lastIdRows = privateMessageMapper.selectMaps(lastMessageWrapper);
        if (lastIdRows == null || lastIdRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> lastIds = lastIdRows.stream()
                .map(row -> row.get("last_id"))
                .filter(Objects::nonNull)
                .map(value -> ((Number) value).longValue())
                .collect(Collectors.toList());
        List<PrivateMessage> lastMessages = privateMessageMapper.selectBatchIds(lastIds);
        if (lastMessages == null || lastMessages.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 最近一条消息时间倒序，最新的会话排在最前面
        lastMessages.sort(Comparator.comparing(PrivateMessage::getId).reversed());

        Set<String> conversationKeys = lastMessages.stream()
                .map(PrivateMessage::getConversationKey)
                .collect(Collectors.toSet());

        // 3. 一次查出各会话中「我未读」的条数
        QueryWrapper<PrivateMessage> unreadWrapper = new QueryWrapper<>();
        unreadWrapper.select("conversation_key", "COUNT(*) AS unread_count")
                .eq("to_user_id", me)
                .eq("is_read", PrivateMessageConstant.UNREAD)
                .in("conversation_key", conversationKeys)
                .groupBy("conversation_key");
        Map<String, Integer> unreadMap = new HashMap<>();
        List<Map<String, Object>> unreadRows = privateMessageMapper.selectMaps(unreadWrapper);
        if (unreadRows != null) {
            for (Map<String, Object> row : unreadRows) {
                Object key = row.get("conversation_key");
                Object count = row.get("unread_count");
                if (key != null && count != null) {
                    unreadMap.put(key.toString(), Math.toIntExact(((Number) count).longValue()));
                }
            }
        }

        // 4. 批量回查对方用户与被咨询商品，避免逐条查询
        Set<Long> peerIds = lastMessages.stream()
                .map(message -> peerOf(message, me))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = peerIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(peerIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        Set<Long> productIds = lastMessages.stream()
                .map(PrivateMessage::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, product -> product, (a, b) -> a));

        List<ConversationVO> conversations = new ArrayList<>(lastMessages.size());
        for (PrivateMessage lastMessage : lastMessages) {
            Long peerId = peerOf(lastMessage, me);
            User peer = userMap.get(peerId);
            Product product = lastMessage.getProductId() == null ? null
                    : productMap.get(lastMessage.getProductId());

            ConversationVO vo = new ConversationVO();
            vo.setPeerId(peerId);
            vo.setPeerName(resolvePeerName(peer));
            vo.setPeerAvatar(peer == null ? null : peer.getAvatar());
            vo.setProductId(lastMessage.getProductId());
            // 商品可能已被卖家删除，此时只保留 ID，标题封面为空由前端处理
            vo.setProductTitle(product == null ? null : product.getTitle());
            vo.setProductCoverImage(product == null ? null : product.getCoverImage());
            vo.setLastContent(lastMessage.getContent());
            vo.setLastTime(lastMessage.getCreateTime());
            vo.setUnreadCount(unreadMap.getOrDefault(lastMessage.getConversationKey(),
                    PrivateMessageConstant.NO_UNREAD));
            conversations.add(vo);
        }
        return conversations;
    }

    @Override
    public List<PrivateMessageVO> listChat(Long peerId, Long productId) {
        Long me = requireLoginUserId();
        if (peerId == null) {
            throw new BusinessException("对方用户不能为空");
        }
        if (me.equals(peerId)) {
            throw new BusinessException("不能和自己聊天");
        }
        String conversationKey = buildConversationKey(me, peerId, productId);

        List<PrivateMessage> messages = privateMessageMapper.selectList(
                new QueryWrapper<PrivateMessage>()
                        .eq("conversation_key", conversationKey)
                        .orderByAsc("id"));
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 打开会话即把对方发给我的消息标记为已读（会话双方才能算出该键，不存在越权）
        LambdaUpdateWrapper<PrivateMessage> readWrapper = new LambdaUpdateWrapper<>();
        readWrapper.eq(PrivateMessage::getConversationKey, conversationKey)
                .eq(PrivateMessage::getToUserId, me)
                .eq(PrivateMessage::getIsRead, PrivateMessageConstant.UNREAD)
                .set(PrivateMessage::getIsRead, PrivateMessageConstant.READ);
        privateMessageMapper.update(null, readWrapper);

        return messages.stream().map(message -> {
            PrivateMessageVO vo = new PrivateMessageVO();
            vo.setId(message.getId());
            vo.setFromUserId(message.getFromUserId());
            vo.setToUserId(message.getToUserId());
            vo.setProductId(message.getProductId());
            vo.setContent(message.getContent());
            vo.setCreateTime(message.getCreateTime());
            // 本轮刚标记完已读，返回给前端时按已读展示，避免气泡状态与列表不一致
            vo.setIsRead(PrivateMessageConstant.READ);
            vo.setSelf(me.equals(message.getFromUserId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Integer getUnreadCount() {
        Long me = requireLoginUserId();
        return Math.toIntExact(privateMessageMapper.selectCount(
                new QueryWrapper<PrivateMessage>()
                        .eq("to_user_id", me)
                        .eq("is_read", PrivateMessageConstant.UNREAD)));
    }

    @Override
    public void deleteConversation(Long peerId, Long productId) {
        Long me = requireLoginUserId();
        if (peerId == null) {
            throw new BusinessException("对方用户不能为空");
        }
        String conversationKey = buildConversationKey(me, peerId, productId);
        // 按会话键删除即双向删除：对方的会话列表里同样不再出现
        privateMessageMapper.delete(new QueryWrapper<PrivateMessage>()
                .eq("conversation_key", conversationKey));
        log.info("会话已删除，用户：{}，对方：{}，关联商品：{}", me, peerId, productId);
    }

    /**
     * 构造会话键
     * <p>
     * 双方用户 ID 排序后拼接，保证 a 发给 b 与 b 发给 a 落在同一个会话；
     * 商品 ID 参与拼接，使「同一对用户的商品咨询」与「普通私信」互不混淆
     *
     * @param oneUserId  会话一方用户 ID
     * @param otherUserId 会话另一方用户 ID
     * @param productId  关联商品 ID（可为空）
     * @return 会话键
     */
    private String buildConversationKey(Long oneUserId, Long otherUserId, Long productId) {
        long first = Math.min(oneUserId, otherUserId);
        long second = Math.max(oneUserId, otherUserId);
        long productPart = productId == null
                ? PrivateMessageConstant.NO_PRODUCT_PLACEHOLDER : productId;
        return first + PrivateMessageConstant.CONVERSATION_KEY_SEPARATOR
                + second + PrivateMessageConstant.CONVERSATION_KEY_SEPARATOR
                + productPart;
    }

    /**
     * 取会话中的对方用户 ID
     */
    private Long peerOf(PrivateMessage message, Long me) {
        return me.equals(message.getFromUserId()) ? message.getToUserId() : message.getFromUserId();
    }

    /**
     * 对方昵称：未设置昵称时回落为用户名；用户已被删除时给出统一提示
     */
    private String resolvePeerName(User peer) {
        if (peer == null) {
            return "已注销用户";
        }
        return StringUtils.hasText(peer.getNickname()) ? peer.getNickname() : peer.getUsername();
    }

    /**
     * 取当前登录用户 ID，未登录直接抛 401
     */
    private Long requireLoginUserId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }
}
