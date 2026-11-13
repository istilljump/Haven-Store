package com.example.order.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.common.ResultCodeEnum;
import com.example.order.constant.OrderConstant;
import com.example.order.dto.CreateOrderDTO;
import com.example.order.entity.OrderItem;
import com.example.order.entity.ProductOrder;
import com.example.order.enums.OrderStatusEnum;
import com.example.order.mapper.OrderItemMapper;
import com.example.order.mapper.ProductOrderMapper;
import com.example.order.service.OrderService;
import com.example.order.vo.OrderItemVO;
import com.example.order.vo.OrderVO;
import com.example.product.constant.RelationTypeConstant;
import com.example.product.entity.Product;
import com.example.product.entity.UserProductRelation;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.example.product.mapper.UserProductRelationMapper;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.example.search.service.SearchLogService;

/**
 * 订单模块业务逻辑实现类
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 订单号中的时间部分格式（精确到毫秒 + 3 位随机数，足够避免同秒并发撞号） */
    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 订单数据访问对象 */
    private final ProductOrderMapper productOrderMapper;

    /** 订单明细数据访问对象 */
    private final OrderItemMapper orderItemMapper;

    /** 商品数据访问对象（校验状态、锁定商品、回填商品当前状态） */
    private final ProductMapper productMapper;

    /** 用户数据访问对象（回填卖家昵称） */
    private final UserMapper userMapper;

    /** 用户-商品关联数据访问对象（下单后把商品移出购物车） */
    private final UserProductRelationMapper userProductRelationMapper;

    /** 搜索埋点服务（成交归因用；order→search 单向依赖，失败不影响支付） */
    private final com.example.search.service.SearchLogService searchLogService;

    /** 交易事件通知服务（轮8：下单/支付/取消/完成等事件定向通知买卖双方与收藏者） */
    private final com.example.message.service.TradeNotifyService tradeNotifyService;

    /** 系统设置服务（轮8：面交码/议价总开关读取） */
    private final com.example.admin.service.SystemSettingService systemSettingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO dto) {
        Long buyerId = requireLoginUserId();
        // 1. 去重并保持稳定顺序，同一商品被勾选两次只算一件
        List<Long> productIds = dto.getProductIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            throw new BusinessException("请选择要购买的商品");
        }
        if (productIds.size() > OrderConstant.MAX_ITEM_COUNT) {
            throw new BusinessException("单笔订单最多结算 " + OrderConstant.MAX_ITEM_COUNT + " 件商品");
        }

        List<Product> products = productMapper.selectBatchIds(productIds);
        if (products.size() != productIds.size()) {
            throw new BusinessException("部分商品已不存在，请刷新购物车后重试");
        }

        // 2. 先整体校验再落库：避免创建到一半才发现某件商品买不了
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Product product : products) {
            if (buyerId.equals(product.getUserId())) {
                throw new BusinessException("「" + product.getTitle() + "」是你自己发布的商品，不能购买");
            }
            if (!ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())) {
                String desc = ProductStatusEnum.OFF_SHELF.getCode().equals(product.getStatus())
                        ? "已下架" : "已售出";
                throw new BusinessException("「" + product.getTitle() + "」" + desc
                        + "，请从购物车移除后再结算");
            }
            totalAmount = totalAmount.add(product.getPrice());
        }

        // 3. 落订单主表
        ProductOrder order = new ProductOrder();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setTotalAmount(totalAmount);
        order.setItemCount(productIds.size());
        order.setStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        productOrderMapper.insert(order);

        // 4. 锁定商品：条件更新（仅当仍在售时才能锁定），更新不到说明刚被别人买走
        for (Product product : products) {
            int changed = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, product.getId())
                    .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                    .set(Product::getStatus, ProductStatusEnum.SOLD.getCode()));
            if (changed == 0) {
                throw new BusinessException("「" + product.getTitle() + "」刚刚被其他买家买走了，请从购物车移除");
            }
        }

        // 5. 写订单明细（标题、封面、价格留快照，商品之后被改被删都不影响这笔订单）
        List<OrderItem> items = new ArrayList<>(products.size());
        for (Product product : products) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setSellerId(product.getUserId());
            item.setTitle(product.getTitle());
            item.setCoverImage(product.getCoverImage());
            item.setPrice(product.getPrice());
            orderItemMapper.insert(item);
            items.add(item);
        }

        // 6. 已下单的商品从购物车移除，避免购物车里留着已买走的东西
        userProductRelationMapper.delete(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getUserId, buyerId)
                .eq(UserProductRelation::getRelationType, RelationTypeConstant.CART)
                .in(UserProductRelation::getProductId, productIds));

        // 7. 交易事件通知（轮8）：通知各卖家「商品被下单锁定」；通知收藏者「收藏的商品已被买走」
        notifyOrderCreated(order, products, buyerId);

        log.info("订单创建成功，订单号：{}，买家ID：{}，金额：{}，商品数：{}",
                order.getOrderNo(), buyerId, totalAmount, productIds.size());
        return toOrderVO(order, items, displayName(userMapper.selectById(buyerId)));
    }

    /**
     * 下单事件通知（轮8）：通知卖家与收藏者（try/catch，通知失败不影响下单事务）
     */
    private void notifyOrderCreated(ProductOrder order, List<Product> products, Long buyerId) {
        try {
            // 卖家通知：按卖家分组，说明锁定与超时释放规则
            Map<Long, List<Product>> bySeller = products.stream()
                    .filter(p -> p.getUserId() != null)
                    .collect(Collectors.groupingBy(Product::getUserId));
            bySeller.forEach((sellerId, list) -> {
                String titles = list.stream().map(Product::getTitle)
                        .reduce((a, b) -> a + "、" + b).orElse("");
                tradeNotifyService.notifyUser(sellerId, "你的商品有人下单了",
                        "商品「" + titles + "」已被买家下单锁定（订单号 " + order.getOrderNo()
                                + "）。买家支付后请准备面交；若 30 分钟内未支付，订单将自动取消、商品自动恢复在售。");
            });
            // 收藏者通知：每件商品各自通知其收藏者（排除买家本人；售出由商品锁定事件天然保证一次）
            for (Product product : products) {
                List<Long> collectorIds = findCollectorIds(product.getId());
                collectorIds.remove(buyerId);
                if (!collectorIds.isEmpty()) {
                    tradeNotifyService.notifyUsers(collectorIds, "收藏的「" + product.getTitle() + "」已被买走",
                            "你收藏的「" + product.getTitle() + "」已被其他同学下单买走。喜欢同类闲置？去商品详情页看看同校区同类推荐。");
                }
            }
        } catch (Exception e) {
            // 通知属于增强信息，失败只告警不影响下单事务
            log.warn("下单事件通知异常（忽略）：{}", e.getMessage());
        }
    }

    /** 查询商品的收藏者ID列表（去重；无收藏返回空列表） */
    private List<Long> findCollectorIds(Long productId) {
        return userProductRelationMapper.selectList(new LambdaQueryWrapper<UserProductRelation>()
                        .eq(UserProductRelation::getProductId, productId)
                        .eq(UserProductRelation::getRelationType, RelationTypeConstant.COLLECT))
                .stream().map(UserProductRelation::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 按议价价创建单商品订单（轮8：买家接受私信报价后由 message 模块调用）
     * <p>
     * 复用购物车下单的校验与锁定语义；差异仅在成交价取议价价——
     * order_item 快照价即议价价，随支付状态回流行情样本（议价也是真实成交）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createFromOffer(Long productId, Long buyerId, BigDecimal negotiatedPrice) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (buyerId.equals(product.getUserId())) {
            throw new BusinessException("不能购买自己发布的商品");
        }
        if (!ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())) {
            String desc = ProductStatusEnum.OFF_SHELF.getCode().equals(product.getStatus())
                    ? "已下架" : "已售出";
            throw new BusinessException("「" + product.getTitle() + "」" + desc + "，报价已失效");
        }
        if (negotiatedPrice == null || negotiatedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("议价价格不合法");
        }

        // 落订单主表（总价即议价价）
        ProductOrder order = new ProductOrder();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setTotalAmount(negotiatedPrice);
        order.setItemCount(1);
        order.setStatus(OrderStatusEnum.PENDING_PAY.getCode());
        productOrderMapper.insert(order);

        // CAS 锁定商品（与购物车下单同一语义：并发下更新不到即已被买走）
        int changed = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, product.getId())
                .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                .set(Product::getStatus, ProductStatusEnum.SOLD.getCode()));
        if (changed == 0) {
            throw new BusinessException("「" + product.getTitle() + "」刚刚被其他买家买走了，报价已失效");
        }

        // 订单明细：成交快照价取议价价
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setSellerId(product.getUserId());
        item.setTitle(product.getTitle());
        item.setCoverImage(product.getCoverImage());
        item.setPrice(negotiatedPrice);
        orderItemMapper.insert(item);

        // 该商品若在买家购物车中，同步移除
        userProductRelationMapper.delete(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getUserId, buyerId)
                .eq(UserProductRelation::getRelationType, RelationTypeConstant.CART)
                .eq(UserProductRelation::getProductId, productId));

        // 交易事件通知（复用下单通知：卖家锁定提醒 + 收藏者售出提醒）
        notifyOrderCreated(order, List.of(product), buyerId);

        log.info("议价订单创建成功，订单号：{}，买家：{}，商品：{}，议价价：{}",
                order.getOrderNo(), buyerId, productId, negotiatedPrice);
        return toOrderVO(order, List.of(item), displayName(userMapper.selectById(buyerId)));
    }

    @Override
    public Page<OrderVO> listMyOrders(Integer status, Integer page, Integer pageSize) {
        Long buyerId = requireLoginUserId();
        LambdaQueryWrapper<ProductOrder> wrapper = new LambdaQueryWrapper<ProductOrder>()
                .eq(ProductOrder::getBuyerId, buyerId)
                .orderByDesc(ProductOrder::getCreateTime)
                .orderByDesc(ProductOrder::getId);
        if (status != null) {
            wrapper.eq(ProductOrder::getStatus, status);
        }
        Page<ProductOrder> orderPage = productOrderMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Page<OrderVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(buildOrderVOs(orderPage.getRecords()));
        return result;
    }

    /**
     * 卖家视角：包含我发布的商品的订单
     * <p>
     * 需要先在明细表里找出涉及我的订单号，再取订单，无法交给单表分页，
     * 因此先取全量订单号再内存分页（单个用户的订单量级很小，代价可接受，
     * 与「我的收藏」的处理方式一致）
     */
    @Override
    public Page<OrderVO> listSoldOrders(Integer page, Integer pageSize) {
        Long sellerId = requireLoginUserId();
        List<Long> orderIds = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getSellerId, sellerId))
                .stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        int total = orderIds.size();
        if (total == 0) {
            return new Page<>(page, pageSize, 0);
        }

        List<ProductOrder> orders = productOrderMapper.selectBatchIds(orderIds).stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());

        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<ProductOrder> pageRecords = from >= total
                ? Collections.emptyList() : orders.subList(from, to);

        Page<OrderVO> result = new Page<>(page, pageSize, total);
        result.setRecords(buildOrderVOs(pageRecords));
        return result;
    }

    @Override
    public OrderVO getOrderDetail(String orderNo) {
        Long userId = requireLoginUserId();
        ProductOrder order = getOrderByNoOrThrow(orderNo);
        // 买家本人可见；非买家但订单中有自己发布的商品（卖家）也可查看，
        // 便于卖家核对「谁买走了我的东西」，越权兜底仍在本方法内完成
        boolean isBuyer = userId.equals(order.getBuyerId());
        if (!isBuyer) {
            Long sellerItemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
                    .eq(OrderItem::getSellerId, userId));
            if (sellerItemCount == null || sellerItemCount == 0) {
                throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能查看自己的订单");
            }
        }
        OrderVO vo = toOrderVO(order, listItems(order.getId()),
                displayName(userMapper.selectById(order.getBuyerId())));
        // 轮8：面交码仅对「买家本人 + 已支付」可见（卖家通过输码弹窗获得码，不直接下发）
        if (isBuyer && OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
            vo.setConfirmCode(order.getConfirmCode());
        }
        vo.setConfirmedBy(order.getConfirmedBy());
        return vo;
    }

    @Override
    public void pay(String orderNo) {
        ProductOrder order = requireMyOrder(orderNo);
        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException("订单当前为「" + OrderStatusEnum.descOf(order.getStatus()) + "」，不能支付");
        }

        // 条件更新：并发下（如定时取消同时发生）状态被别人改过时更新不到，避免重复推进
        // 轮8：支付成功同时生成面交确认码（总开关开启时；6 位数字，订单级一次性）
        boolean codeEnabled = isConfirmCodeEnabled();
        String confirmCode = codeEnabled ? generateConfirmCode() : null;
        int changed = productOrderMapper.update(null, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, order.getId())
                .eq(ProductOrder::getStatus, OrderStatusEnum.PENDING_PAY.getCode())
                .set(ProductOrder::getStatus, OrderStatusEnum.PAID.getCode())
                .set(ProductOrder::getPayTime, LocalDateTime.now())
                .set(confirmCode != null, ProductOrder::getConfirmCode, confirmCode)
                .set(ProductOrder::getConfirmFailCount, 0));
        if (changed == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        // 说明：此处为模拟支付，仅推进订单状态，项目未接入任何真实支付渠道，不产生真实扣款
        log.info("订单模拟支付成功，订单号：{}，买家ID：{}，金额：{}",
                order.getOrderNo(), order.getBuyerId(), order.getTotalAmount());

        // 搜索埋点成交归因（数据飞轮的一环）：把「点击过这些商品」的近期埋点标记为已成交。
        // 事务外 try/catch：归因失败只告警，绝不影响支付主流程（order→search 单向依赖）
        try {
            List<Long> productIds = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()))
                    .stream().map(OrderItem::getProductId).distinct().collect(Collectors.toList());
            searchLogService.markDeals(productIds);
        } catch (Exception e) {
            log.warn("搜索埋点成交归因异常（忽略）：{}", e.getMessage());
        }

        // 支付事件通知（轮8）：买家（含面交码说明）+ 各卖家（待面交）
        notifyOrderPaid(order, confirmCode);
    }

    /** 面交码总开关（缺省开启） */
    private boolean isConfirmCodeEnabled() {
        return !Boolean.FALSE.equals(systemSettingService.get().getTrade().getTradeConfirmCodeEnabled());
    }

    /** 生成 6 位数字面交确认码（订单级，首零允许） */
    private String generateConfirmCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /**
     * 支付事件通知（轮8）：买家收到面交码说明、卖家收到待面交提醒（try/catch 不影响支付）
     */
    private void notifyOrderPaid(ProductOrder order, String confirmCode) {
        try {
            List<OrderItem> items = listItems(order.getId());
            String titles = items.stream().map(OrderItem::getTitle)
                    .reduce((a, b) -> a + "、" + b).orElse("");
            if (confirmCode != null) {
                tradeNotifyService.notifyUser(order.getBuyerId(), "支付成功，面交码已生成",
                        "订单 " + order.getOrderNo() + "（「" + titles + "」）已支付。面交时向卖家出示 6 位确认码 "
                                + confirmCode + "，卖家输码后订单即完成；你也可以在收货后手动确认，7 天未确认将自动完成。");
            } else {
                tradeNotifyService.notifyUser(order.getBuyerId(), "支付成功",
                        "订单 " + order.getOrderNo() + "（「" + titles + "」）已支付。请在面交完成后确认收货，7 天未确认将自动完成。");
            }
            // 卖家通知：按明细卖家分组
            Map<Long, List<OrderItem>> bySeller = items.stream()
                    .filter(i -> i.getSellerId() != null)
                    .collect(Collectors.groupingBy(OrderItem::getSellerId));
            bySeller.forEach((sellerId, list) -> {
                String t = list.stream().map(OrderItem::getTitle)
                        .reduce((a, b) -> a + "、" + b).orElse("");
                String codeHint = confirmCode != null
                        ? "买家面交时会出示 6 位确认码，请在「我卖出的」订单详情输入该码完成交付；"
                        : "";
                tradeNotifyService.notifyUser(sellerId, "你的商品已售出，待面交",
                        "订单 " + order.getOrderNo() + "（「" + t + "」）已支付。" + codeHint
                                + "买家确认收货或 7 天后订单也会自动完成。");
            });
        } catch (Exception e) {
            log.warn("支付事件通知异常（忽略）：{}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo) {
        ProductOrder order = requireMyOrder(orderNo);
        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException("订单当前为「" + OrderStatusEnum.descOf(order.getStatus()) + "」，不能取消");
        }

        int changed = productOrderMapper.update(null, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, order.getId())
                .eq(ProductOrder::getStatus, OrderStatusEnum.PENDING_PAY.getCode())
                .set(ProductOrder::getStatus, OrderStatusEnum.CANCELLED.getCode())
                .set(ProductOrder::getCancelTime, LocalDateTime.now()));
        if (changed == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        releaseOrderItems(order.getId());
        log.info("订单已取消，订单号：{}，商品已释放回在售", order.getOrderNo());

        // 取消事件通知（轮8）：通知卖家商品已回架（try/catch 不影响取消流程）
        try {
            List<OrderItem> items = listItems(order.getId());
            Map<Long, List<OrderItem>> bySeller = items.stream()
                    .filter(i -> i.getSellerId() != null)
                    .collect(Collectors.groupingBy(OrderItem::getSellerId));
            bySeller.forEach((sellerId, list) -> {
                String t = list.stream().map(OrderItem::getTitle)
                        .reduce((a, b) -> a + "、" + b).orElse("");
                tradeNotifyService.notifyUser(sellerId, "订单已取消，商品已回架",
                        "订单 " + order.getOrderNo() + "（「" + t + "」）被买家取消，商品已恢复在售。");
            });
        } catch (Exception e) {
            log.warn("取消事件通知异常（忽略）：{}", e.getMessage());
        }
    }

    @Override
    public void confirmFinish(String orderNo) {
        ProductOrder order = requireMyOrder(orderNo);
        if (!OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException("只有已支付的订单才能确认收货");
        }

        int changed = productOrderMapper.update(null, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, order.getId())
                .eq(ProductOrder::getStatus, OrderStatusEnum.PAID.getCode())
                .set(ProductOrder::getStatus, OrderStatusEnum.FINISHED.getCode())
                .set(ProductOrder::getFinishTime, LocalDateTime.now())
                .set(ProductOrder::getConfirmedBy, CONFIRMED_BY_BUYER));
        if (changed == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        log.info("订单已完成，订单号：{}", order.getOrderNo());

        // 完成事件通知（轮8）：通知卖家（含评价引导——评分将回流信用画像）
        try {
            List<OrderItem> items = listItems(order.getId());
            Map<Long, List<OrderItem>> bySeller = items.stream()
                    .filter(i -> i.getSellerId() != null)
                    .collect(Collectors.groupingBy(OrderItem::getSellerId));
            bySeller.forEach((sellerId, list) -> {
                String t = list.stream().map(OrderItem::getTitle)
                        .reduce((a, b) -> a + "、" + b).orElse("");
                tradeNotifyService.notifyUser(sellerId, "订单已完成",
                        "订单 " + order.getOrderNo() + "（「" + t + "」）已被买家确认完成。买家评价后评分将计入你的信用画像，好的评价来自好的面交体验。");
            });
        } catch (Exception e) {
            log.warn("完成事件通知异常（忽略）：{}", e.getMessage());
        }
    }

    /** 完成确认方式：1 买家确认收货 */
    private static final int CONFIRMED_BY_BUYER = 1;
    /** 完成确认方式：2 卖家输码面交 */
    private static final int CONFIRMED_BY_SELLER_CODE = 2;
    /** 完成确认方式：3 超时自动完成 */
    private static final int CONFIRMED_BY_TIMEOUT = 3;
    /** 面交码最大错误尝试次数（超过后卖家输码路径锁定，买家确认路径仍可用） */
    private static final int MAX_CONFIRM_FAIL_COUNT = 5;

    /**
     * 卖家输码面交确认（轮8）：支付成功的订单，卖家输入买家出示的 6 位码即完成交付
     * <p>
     * 校验链：总开关 → 订单存在且当前用户是本单卖家 → 状态为已支付 → 输码未锁定
     * → 码匹配。任一 sellership/状态不满足即拒绝；错码累计 fail_count，达 5 次
     * 后输码路径锁定（买家手动确认路径不受影响）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmByCode(String orderNo, String code) {
        Long sellerId = requireLoginUserId();
        if (!isConfirmCodeEnabled()) {
            throw new BusinessException("面交确认码功能未开启");
        }
        if (!StringUtils.hasText(code) || !code.trim().matches("\\d{6}")) {
            throw new BusinessException("请输入 6 位数字确认码");
        }
        ProductOrder order = getOrderByNoOrThrow(orderNo);
        // 卖家身份校验：当前用户必须是本订单任一明细的卖家
        List<Long> sellerIds = listItems(order.getId()).stream()
                .map(OrderItem::getSellerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!sellerIds.contains(sellerId)) {
            throw new BusinessException("只有该订单的卖家才能输入面交确认码");
        }
        if (!OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException("订单当前为「" + OrderStatusEnum.descOf(order.getStatus()) + "」，无需面交确认");
        }
        int failCount = order.getConfirmFailCount() == null ? 0 : order.getConfirmFailCount();
        if (failCount >= MAX_CONFIRM_FAIL_COUNT) {
            throw new BusinessException("确认码错误次数过多，输码已锁定；请联系买家在其订单中直接确认收货");
        }
        if (!code.trim().equals(order.getConfirmCode())) {
            productOrderMapper.update(null, new LambdaUpdateWrapper<ProductOrder>()
                    .eq(ProductOrder::getId, order.getId())
                    .set(ProductOrder::getConfirmFailCount, failCount + 1));
            throw new BusinessException("确认码不正确（剩余尝试次数 " + (MAX_CONFIRM_FAIL_COUNT - failCount - 1) + " 次）");
        }

        // 码匹配：条件更新完成订单（并发下与买家确认/超时自动互斥，谁先改到谁生效）
        int changed = productOrderMapper.update(null, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, order.getId())
                .eq(ProductOrder::getStatus, OrderStatusEnum.PAID.getCode())
                .set(ProductOrder::getStatus, OrderStatusEnum.FINISHED.getCode())
                .set(ProductOrder::getFinishTime, LocalDateTime.now())
                .set(ProductOrder::getConfirmedBy, CONFIRMED_BY_SELLER_CODE));
        if (changed == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        log.info("订单经卖家输码面交完成，订单号：{}", order.getOrderNo());

        // 完成事件通知（轮8）：通知买家
        try {
            List<OrderItem> items = listItems(order.getId());
            String t = items.stream().map(OrderItem::getTitle)
                    .reduce((a, b) -> a + "、" + b).orElse("");
            tradeNotifyService.notifyUser(order.getBuyerId(), "面交完成，订单已交付",
                    "订单 " + order.getOrderNo() + "（「" + t + "」）经卖家输码确认交付完成。欢迎对本次交易评价，让信用数据帮到下一位同学。");
        } catch (Exception e) {
            log.warn("面交完成通知异常（忽略）：{}", e.getMessage());
        }
    }

    /**
     * 支付后超时自动完成（轮8 兜底）：已支付订单超过 autoFinishDays 天未确认，
     * 由定时任务自动推进为已完成（confirmed_by=3），并通知买卖双方
     */
    @Override
    public int autoFinishPaidOrders(int autoFinishDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(autoFinishDays);
        List<ProductOrder> staleOrders = productOrderMapper.selectList(new LambdaQueryWrapper<ProductOrder>()
                .eq(ProductOrder::getStatus, OrderStatusEnum.PAID.getCode())
                .lt(ProductOrder::getPayTime, deadline));
        int finished = 0;
        for (ProductOrder order : staleOrders) {
            try {
                int changed = productOrderMapper.update(null, new LambdaUpdateWrapper<ProductOrder>()
                        .eq(ProductOrder::getId, order.getId())
                        .eq(ProductOrder::getStatus, OrderStatusEnum.PAID.getCode())
                        .set(ProductOrder::getStatus, OrderStatusEnum.FINISHED.getCode())
                        .set(ProductOrder::getFinishTime, LocalDateTime.now())
                        .set(ProductOrder::getConfirmedBy, CONFIRMED_BY_TIMEOUT));
                if (changed > 0) {
                    finished++;
                    log.info("已支付订单超时自动完成，订单号：{}", order.getOrderNo());
                    try {
                        List<OrderItem> items = listItems(order.getId());
                        String t = items.stream().map(OrderItem::getTitle)
                                .reduce((a, b) -> a + "、" + b).orElse("");
                        tradeNotifyService.notifyUser(order.getBuyerId(), "订单已自动完成",
                                "订单 " + order.getOrderNo() + "（「" + t + "」）支付后 7 天未确认，已自动完成。如对交易有异议请联系平台客服。");
                        List<Long> sellerIds = items.stream().map(OrderItem::getSellerId)
                                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
                        tradeNotifyService.notifyUsers(sellerIds, "订单已自动完成",
                                "订单 " + order.getOrderNo() + "（「" + t + "」）支付后 7 天未确认，已自动完成。");
                    } catch (Exception e) {
                        log.warn("自动完成通知异常（忽略）：{}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                // 单笔失败不影响其余订单
                log.error("自动完成订单失败，订单号：{}", order.getOrderNo(), e);
            }
        }
        return finished;
    }

    /**
     * 取消超时未支付订单并释放商品（供定时任务调用）
     * <p>
     * 逐单条件更新（仅待支付状态可取消），与买家手动的取消操作互不冲突：
     * 谁先把状态改掉谁生效，另一方更新不到即跳过
     */
    @Override
    public int cancelTimeoutOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ProductOrder> expiredOrders = productOrderMapper.selectList(new LambdaQueryWrapper<ProductOrder>()
                .eq(ProductOrder::getStatus, OrderStatusEnum.PENDING_PAY.getCode())
                .lt(ProductOrder::getCreateTime, deadline));
        int cancelled = 0;
        for (ProductOrder order : expiredOrders) {
            try {
                int changed = productOrderMapper.update(null, new LambdaUpdateWrapper<ProductOrder>()
                        .eq(ProductOrder::getId, order.getId())
                        .eq(ProductOrder::getStatus, OrderStatusEnum.PENDING_PAY.getCode())
                        .set(ProductOrder::getStatus, OrderStatusEnum.CANCELLED.getCode())
                        .set(ProductOrder::getCancelTime, LocalDateTime.now()));
                if (changed > 0) {
                    releaseOrderItems(order.getId());
                    cancelled++;
                    log.info("超时未支付订单已自动取消，订单号：{}", order.getOrderNo());
                    // 超时取消事件通知（轮8）：告知买家订单已取消、商品已回架
                    try {
                        String t = listItems(order.getId()).stream().map(OrderItem::getTitle)
                                .reduce((a, b) -> a + "、" + b).orElse("");
                        tradeNotifyService.notifyUser(order.getBuyerId(), "订单超时未支付已取消",
                                "订单 " + order.getOrderNo() + "（「" + t + "」）超过 30 分钟未支付已自动取消，商品已恢复在售。");
                    } catch (Exception e) {
                        log.warn("超时取消通知异常（忽略）：{}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                // 单笔失败不影响其余订单的自动取消
                log.error("自动取消订单失败，订单号：{}", order.getOrderNo(), e);
            }
        }
        return cancelled;
    }

    /**
     * 释放订单占用的商品：仅把仍处于「已售出」的释放回「在售」，不覆盖卖家自己的下架操作
     */
    private void releaseOrderItems(Long orderId) {
        for (OrderItem item : listItems(orderId)) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, item.getProductId())
                    .eq(Product::getStatus, ProductStatusEnum.SOLD.getCode())
                    .set(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode()));
        }
    }

    /**
     * 按订单号取订单，不存在直接报错
     */
    private ProductOrder getOrderByNoOrThrow(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException("订单号不能为空");
        }
        ProductOrder order = productOrderMapper.selectOne(new LambdaQueryWrapper<ProductOrder>()
                .eq(ProductOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /**
     * 按订单号取「我的订单」，非本人订单直接拒绝
     */
    private ProductOrder requireMyOrder(String orderNo) {
        Long buyerId = requireLoginUserId();
        ProductOrder order = getOrderByNoOrThrow(orderNo);
        if (!buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能操作自己的订单");
        }
        return order;
    }

    /**
     * 批量把订单转成展示对象（一次补齐明细、买家卖家昵称与商品当前状态，避免逐条查询）
     */
    private List<OrderVO> buildOrderVOs(List<ProductOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(ProductOrder::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));
        Map<Long, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        // 买家昵称一次查完，不能放进下面的 map 里逐条查，否则就是 N+1
        Map<Long, String> buyerNames = resolveBuyerNames(orders);

        return orders.stream()
                .map(order -> toOrderVO(order,
                        itemsByOrder.getOrDefault(order.getId(), Collections.emptyList()),
                        buyerNames.get(order.getBuyerId())))
                .collect(Collectors.toList());
    }

    /**
     * 查询订单明细
     */
    private List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    /**
     * 批量回查买家昵称，key 为买家 ID
     */
    private Map<Long, String> resolveBuyerNames(List<ProductOrder> orders) {
        List<Long> buyerIds = orders.stream()
                .map(ProductOrder::getBuyerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (buyerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(buyerIds).stream()
                .collect(Collectors.toMap(User::getId, this::displayName, (a, b) -> a));
    }

    /**
     * 用户展示名：昵称优先，未设置昵称回落为用户名，用户已删除给出统一提示
     */
    private String displayName(User user) {
        if (user == null) {
            return "已注销用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    /**
     * 单个订单转展示对象
     *
     * @param order     订单
     * @param items     订单明细
     * @param buyerName 买家昵称（卖家视角需要展示"谁买走了"）
     */
    private OrderVO toOrderVO(ProductOrder order, List<OrderItem> items, String buyerName) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBuyerId(order.getBuyerId());
        vo.setBuyerName(buyerName);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setItemCount(order.getItemCount());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatusEnum.descOf(order.getStatus()));
        vo.setRemark(order.getRemark());
        vo.setPayTime(order.getPayTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCreateTime(order.getCreateTime());

        if (items == null || items.isEmpty()) {
            vo.setItems(Collections.emptyList());
            return vo;
        }

        // 批量回查卖家昵称与商品当前状态（商品可能已被删除）
        List<Long> sellerIds = items.stream().map(OrderItem::getSellerId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> sellerMap = sellerIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(sellerIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
        List<Long> productIds = items.stream().map(OrderItem::getProductId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, product -> product, (a, b) -> a));

        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setProductId(item.getProductId());
            itemVO.setSellerId(item.getSellerId());
            User seller = sellerMap.get(item.getSellerId());
            itemVO.setSellerName(displayName(seller));
            itemVO.setTitle(item.getTitle());
            itemVO.setCoverImage(item.getCoverImage());
            itemVO.setPrice(item.getPrice());
            Product product = productMap.get(item.getProductId());
            itemVO.setProductDeleted(product == null);
            itemVO.setProductStatus(product == null ? null : product.getStatus());
            return itemVO;
        }).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 生成订单号：HM + 毫秒级时间 + 3 位随机数
     */
    private String generateOrderNo() {
        return OrderConstant.ORDER_NO_PREFIX
                + LocalDateTime.now().format(ORDER_NO_TIME_FORMAT)
                + ThreadLocalRandom.current().nextInt(100, 1000);
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
