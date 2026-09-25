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

        log.info("订单创建成功，订单号：{}，买家ID：{}，金额：{}，商品数：{}",
                order.getOrderNo(), buyerId, totalAmount, productIds.size());
        return toOrderVO(order, items, displayName(userMapper.selectById(buyerId)));
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
        Long buyerId = requireLoginUserId();
        ProductOrder order = getOrderByNoOrThrow(orderNo);
        if (!buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "只能查看自己的订单");
        }
        return toOrderVO(order, listItems(order.getId()),
                displayName(userMapper.selectById(order.getBuyerId())));
    }

    @Override
    public void pay(String orderNo) {
        ProductOrder order = requireMyOrder(orderNo);
        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException("订单当前为「" + OrderStatusEnum.descOf(order.getStatus()) + "」，不能支付");
        }

        ProductOrder update = new ProductOrder();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.PAID.getCode());
        update.setPayTime(LocalDateTime.now());
        productOrderMapper.updateById(update);

        // 说明：此处为模拟支付，仅推进订单状态，项目未接入任何真实支付渠道，不产生真实扣款
        log.info("订单模拟支付成功，订单号：{}，买家ID：{}，金额：{}",
                order.getOrderNo(), order.getBuyerId(), order.getTotalAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo) {
        ProductOrder order = requireMyOrder(orderNo);
        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException("订单当前为「" + OrderStatusEnum.descOf(order.getStatus()) + "」，不能取消");
        }

        ProductOrder update = new ProductOrder();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.CANCELLED.getCode());
        update.setCancelTime(LocalDateTime.now());
        productOrderMapper.updateById(update);

        // 释放商品：仅把仍处于「已售出」的释放回「在售」，不覆盖卖家自己的下架操作
        for (OrderItem item : listItems(order.getId())) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, item.getProductId())
                    .eq(Product::getStatus, ProductStatusEnum.SOLD.getCode())
                    .set(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode()));
        }
        log.info("订单已取消，订单号：{}，商品已释放回在售", order.getOrderNo());
    }

    @Override
    public void confirmFinish(String orderNo) {
        ProductOrder order = requireMyOrder(orderNo);
        if (!OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException("只有已支付的订单才能确认收货");
        }

        ProductOrder update = new ProductOrder();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.FINISHED.getCode());
        update.setFinishTime(LocalDateTime.now());
        productOrderMapper.updateById(update);
        log.info("订单已完成，订单号：{}", order.getOrderNo());
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
