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
import org.springframework.transaction.annotation.Transactional;
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

    /** 系统设置服务（轮8：议价总开关） */
    private final com.example.admin.service.SystemSettingService systemSettingService;

    /** 订单服务（轮8：买家接受报价后按议价价创建订单） */
    private final com.example.order.service.OrderService orderService;

    /** 交易事件通知服务（轮8：报价/接受事件通知对方） */
    private final com.example.message.service.TradeNotifyService tradeNotifyService;

    /** 报价有效期（分钟，与订单待支付超时对齐） */
    private static final int OFFER_EXPIRE_MINUTES = 30;

    /** 报价状态：待处理 */
    private static final Integer OFFER_PENDING = 0;
    /** 报价状态：已接受 */
    private static final Integer OFFER_ACCEPTED = 1;
    /** 报价状态：已失效 */
    private static final Integer OFFER_EXPIRED = 2;

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
        // 3. 商品咨询需校验商品存在，并按商品归属决定谁能开口：
        //    - 买家（非发布者）：只能咨询该商品的发布者，否则会话会挂在与买家无关的商品上；
        //    - 卖家（发布者本人）：只允许在「该商品会话已存在」的前提下回复。
        //      卖家在商品会话里回话必须带商品 ID（会话键含商品），一律拒绝会让卖家永远无法回话；
        //      但也不能完全放开，否则从自己的商品详情页点「私信联系」就能凭空开出一条自我咨询会话。
        Long productId = dto.getProductId();
        if (productId != null) {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException("咨询的商品不存在");
            }
            if (me.equals(product.getUserId())) {
                if (!hasConversationAboutProduct(me, dto.getToUserId(), productId)) {
                    throw new BusinessException("不能咨询自己发布的商品");
                }
            } else if (!dto.getToUserId().equals(product.getUserId())) {
                throw new BusinessException("只能向该商品的发布者发起咨询");
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
            // 轮8：议价报价消息回填报价三字段（非报价消息为 null，前端按普通气泡渲染）
            vo.setOfferPrice(message.getOfferPrice());
            vo.setOfferExpireTime(message.getOfferExpireTime());
            vo.setOfferStatus(message.getOfferStatus());
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
     * 判断两个用户之间是否已存在某个商品会话
     * <p>
     * 复用会话键而不是按收发方向拼条件：会话键本身就是「双方 + 商品」的同一性标识，
     * 用它查一次即可，既与 listConversations / deleteConversation 的口径一致，
     * 也不会因为 OR 条件写错而漏判
     *
     * @param oneUserId   一方用户 ID
     * @param otherUserId 另一方用户 ID
     * @param productId   商品 ID
     * @return 该会话已有任意一条消息时返回 true
     */
    private boolean hasConversationAboutProduct(Long oneUserId, Long otherUserId, Long productId) {
        QueryWrapper<PrivateMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("conversation_key", buildConversationKey(oneUserId, otherUserId, productId));
        Long count = privateMessageMapper.selectCount(wrapper);
        return count != null && count > 0;
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

    // ==================== 议价（轮8 业务迭代新增） ====================

    @Override
    public Long sendOffer(com.example.message.dto.SendOfferDTO dto) {
        Long me = requireLoginUserId();
        // 1. 议价总开关（后台交易设置，缺省开启）
        if (Boolean.FALSE.equals(systemSettingService.get().getTrade().getTradeNegotiationEnabled())) {
            throw new BusinessException("议价功能未开启");
        }
        // 2. 只有商品发布者（卖家）能发报价——与「定价权在卖家」的产品叙事一致
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!me.equals(product.getUserId())) {
            throw new BusinessException("只有卖家可以发送报价");
        }
        if (dto.getToUserId().equals(me)) {
            throw new BusinessException("接收人不能是自己");
        }
        // 3. 双方必须已存在该商品会话（报价挂在会话内，不允许凭空开新会话报价）
        if (!hasConversationAboutProduct(me, dto.getToUserId(), dto.getProductId())) {
            throw new BusinessException("该买家尚未咨询过此商品，无法发送报价");
        }
        // 4. 商品必须在售（已锁定/已下架无法议价）
        if (!com.example.product.enums.ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())) {
            throw new BusinessException("商品当前不在售，无法报价");
        }
        // 5. 报价金额：大于 0 且低于现价（让利才构成议价；定价权仍在卖家）
        java.math.BigDecimal price = dto.getPrice();
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("报价金额必须大于 0");
        }
        if (product.getPrice() != null && price.compareTo(product.getPrice()) >= 0) {
            throw new BusinessException("报价须低于商品现价 ¥" + product.getPrice());
        }
        // 6. 同会话旧待处理报价置失效（一次只有一个有效报价）
        expirePendingOffers(buildConversationKey(me, dto.getToUserId(), dto.getProductId()));

        // 7. 落报价消息（content 为自动说明文案，前端以报价卡片渲染）
        PrivateMessage message = new PrivateMessage();
        message.setConversationKey(buildConversationKey(me, dto.getToUserId(), dto.getProductId()));
        message.setFromUserId(me);
        message.setToUserId(dto.getToUserId());
        message.setProductId(dto.getProductId());
        message.setContent("卖家发起议价：¥" + price + "（原价 ¥" + product.getPrice() + "），点击卡片可直接按此价下单");
        message.setOfferPrice(price);
        message.setOfferExpireTime(java.time.LocalDateTime.now().plusMinutes(OFFER_EXPIRE_MINUTES));
        message.setOfferStatus(OFFER_PENDING);
        message.setIsRead(PrivateMessageConstant.UNREAD);
        privateMessageMapper.insert(message);
        log.info("议价报价发送成功，消息ID：{}，卖家：{}，买家：{}，商品：{}，报价：{}",
                message.getId(), me, dto.getToUserId(), dto.getProductId(), price);

        // 8. 通知买家收到报价（交易消息）
        tradeNotifyService.notifyUser(dto.getToUserId(), "收到议价报价",
                "卖家就「" + product.getTitle() + "」向你发起议价 ¥" + price
                        + "（原价 ¥" + product.getPrice() + "），30 分钟内有效，请在私信中查看并决定是否接受。");
        return message.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.example.order.vo.OrderVO acceptOffer(Long messageId) {
        Long me = requireLoginUserId();
        // 1. 议价总开关
        if (Boolean.FALSE.equals(systemSettingService.get().getTrade().getTradeNegotiationEnabled())) {
            throw new BusinessException("议价功能未开启");
        }
        // 2. 消息存在且是报价消息
        PrivateMessage offer = privateMessageMapper.selectById(messageId);
        if (offer == null || offer.getOfferPrice() == null) {
            throw new BusinessException("报价不存在");
        }
        // 3. 只有报价的接收人（买家）能接受
        if (!me.equals(offer.getToUserId())) {
            throw new BusinessException("只有报价的接收人可以接受报价");
        }
        // 4. 状态校验：待处理且未过期（惰性置失效后按过期拒绝）
        if (!OFFER_PENDING.equals(offer.getOfferStatus())) {
            throw new BusinessException("该报价已被接受或已失效");
        }
        if (offer.getOfferExpireTime() != null && offer.getOfferExpireTime().isBefore(java.time.LocalDateTime.now())) {
            privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getId, messageId)
                    .set(PrivateMessage::getOfferStatus, OFFER_EXPIRED));
            throw new BusinessException("报价已过期（有效期 30 分钟），请让卖家重新报价");
        }
        // 5. 商品仍在售
        Product product = productMapper.selectById(offer.getProductId());
        if (product == null
                || !com.example.product.enums.ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())) {
            privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getId, messageId)
                    .set(PrivateMessage::getOfferStatus, OFFER_EXPIRED));
            throw new BusinessException("商品已不在售，报价失效");
        }

        // 6. 置为已接受，并按议价价创建订单（成交快照即议价价，回流行情样本）
        privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getId, messageId)
                .eq(PrivateMessage::getOfferStatus, OFFER_PENDING)
                .set(PrivateMessage::getOfferStatus, OFFER_ACCEPTED));
        com.example.order.vo.OrderVO order = orderService.createFromOffer(offer.getProductId(), me, offer.getOfferPrice());

        // 7. 通知卖家报价被接受（订单号供核对）
        tradeNotifyService.notifyUser(offer.getFromUserId(), "你的报价已被接受",
                "买家已接受你对「" + product.getTitle() + "」的报价 ¥" + offer.getOfferPrice()
                        + "，订单 " + order.getOrderNo() + " 已创建。买家支付后请准备面交。");
        log.info("报价被接受，消息ID：{}，买家：{}，订单：{}", messageId, me, order.getOrderNo());
        return order;
    }

    /** 把某会话内旧的待处理报价置为失效（一次只有一个有效报价） */
    private void expirePendingOffers(String conversationKey) {
        privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationKey, conversationKey)
                .eq(PrivateMessage::getOfferStatus, OFFER_PENDING)
                .set(PrivateMessage::getOfferStatus, OFFER_EXPIRED));
    }
}
