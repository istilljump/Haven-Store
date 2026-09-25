package com.example.cart.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.cart.service.CartService;
import com.example.cart.vo.AddFavoritesResultVO;
import com.example.cart.vo.CartItemVO;
import com.example.common.BusinessException;
import com.example.common.ResultCodeEnum;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 购物车模块业务逻辑实现类
 * <p>
 * 数据落在 user_product_relation 表的 relation_type = cart 上，
 * 与「收藏」是同一张表的两种关系，因此互转只是删一条、加一条
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    /** 用户-商品关联数据访问对象（购物车与收藏共用） */
    private final UserProductRelationMapper userProductRelationMapper;

    /** 商品数据访问对象 */
    private final ProductMapper productMapper;

    /** 用户数据访问对象（回填卖家昵称） */
    private final UserMapper userMapper;

    @Override
    public Boolean addToCart(Long productId) {
        Long userId = requireLoginUserId();
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在或已被删除");
        }
        // 自己发布的商品没有购买意义，提前拦住并说清原因
        if (userId.equals(product.getUserId())) {
            throw new BusinessException("不能把自己发布的商品加入购物车");
        }
        if (!ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())) {
            throw new BusinessException("商品" + statusDesc(product.getStatus()) + "，无法加入购物车");
        }
        // 幂等：已在购物车中直接返回 false，不重复插入也不报错
        if (relationExists(userId, productId, RelationTypeConstant.CART)) {
            return false;
        }
        insertRelation(userId, productId, RelationTypeConstant.CART);
        log.info("加入购物车，用户ID：{}，商品ID：{}", userId, productId);
        return true;
    }

    @Override
    public void removeFromCart(Long productId) {
        Long userId = requireLoginUserId();
        userProductRelationMapper.delete(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getUserId, userId)
                .eq(UserProductRelation::getProductId, productId)
                .eq(UserProductRelation::getRelationType, RelationTypeConstant.CART));
    }

    @Override
    public List<CartItemVO> listCart() {
        Long userId = requireLoginUserId();
        List<UserProductRelation> relations = userProductRelationMapper.selectList(
                new LambdaQueryWrapper<UserProductRelation>()
                        .eq(UserProductRelation::getUserId, userId)
                        .eq(UserProductRelation::getRelationType, RelationTypeConstant.CART)
                        .orderByDesc(UserProductRelation::getCreateTime)
                        .orderByDesc(UserProductRelation::getId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = relations.stream()
                .map(UserProductRelation::getProductId)
                .collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (a, b) -> a));

        Set<Long> sellerIds = productMap.values().stream()
                .map(Product::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> sellerMap = sellerIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(sellerIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        List<CartItemVO> items = new ArrayList<>(relations.size());
        for (UserProductRelation relation : relations) {
            Product product = productMap.get(relation.getProductId());
            CartItemVO vo = new CartItemVO();
            vo.setProductId(relation.getProductId());
            vo.setCreateTime(relation.getCreateTime());
            if (product == null) {
                // 商品已被删除：条目保留但明确不可结算，由用户自己决定是否清理
                vo.setTitle("商品已被删除");
                vo.setStatus(null);
                vo.setAvailable(false);
                vo.setUnavailableReason("商品已被删除");
                items.add(vo);
                continue;
            }
            vo.setTitle(product.getTitle());
            vo.setCoverImage(product.getCoverImage());
            vo.setPrice(product.getPrice());
            vo.setOriginalPrice(product.getOriginalPrice());
            vo.setProductCondition(product.getProductCondition());
            vo.setTradeType(product.getTradeType());
            vo.setStatus(product.getStatus());
            vo.setSellerId(product.getUserId());
            vo.setSellerName(resolveNickname(sellerMap.get(product.getUserId())));

            if (userId.equals(product.getUserId())) {
                vo.setAvailable(false);
                vo.setUnavailableReason("这是你自己发布的商品");
            } else if (!ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())) {
                vo.setAvailable(false);
                vo.setUnavailableReason("商品" + statusDesc(product.getStatus()));
            } else {
                vo.setAvailable(true);
            }
            items.add(vo);
        }
        return items;
    }

    @Override
    public Integer countCart() {
        Long userId = requireLoginUserId();
        return Math.toIntExact(userProductRelationMapper.selectCount(
                new LambdaQueryWrapper<UserProductRelation>()
                        .eq(UserProductRelation::getUserId, userId)
                        .eq(UserProductRelation::getRelationType, RelationTypeConstant.CART)));
    }

    @Override
    public void moveToFavorite(Long productId) {
        Long userId = requireLoginUserId();
        // 先确保收藏关系存在，再删购物车关系，避免中途失败把商品弄丢
        if (!relationExists(userId, productId, RelationTypeConstant.COLLECT)) {
            insertRelation(userId, productId, RelationTypeConstant.COLLECT);
        }
        removeFromCart(productId);
        log.info("购物车移入收藏，用户ID：{}，商品ID：{}", userId, productId);
    }

    @Override
    public AddFavoritesResultVO addAllFavoritesToCart() {
        Long userId = requireLoginUserId();
        List<Long> favoriteIds = userProductRelationMapper.selectList(
                        new LambdaQueryWrapper<UserProductRelation>()
                                .eq(UserProductRelation::getUserId, userId)
                                .eq(UserProductRelation::getRelationType, RelationTypeConstant.COLLECT)
                                .orderByDesc(UserProductRelation::getCreateTime))
                .stream()
                .map(UserProductRelation::getProductId)
                .collect(Collectors.toList());

        AddFavoritesResultVO result = new AddFavoritesResultVO();
        result.setAdded(0);
        result.setSkipped(0);
        if (favoriteIds.isEmpty()) {
            return result;
        }

        Set<Long> alreadyInCart = new HashSet<>(userProductRelationMapper.selectList(
                        new LambdaQueryWrapper<UserProductRelation>()
                                .eq(UserProductRelation::getUserId, userId)
                                .eq(UserProductRelation::getRelationType, RelationTypeConstant.CART))
                .stream()
                .map(UserProductRelation::getProductId)
                .collect(Collectors.toList()));

        Map<Long, Product> productMap = productMapper.selectBatchIds(favoriteIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (a, b) -> a));

        int added = 0;
        int skipped = 0;
        for (Long productId : favoriteIds) {
            Product product = productMap.get(productId);
            boolean buyable = product != null
                    && ProductStatusEnum.ON_SHELF.getCode().equals(product.getStatus())
                    && !userId.equals(product.getUserId());
            if (alreadyInCart.contains(productId)) {
                // 已经在购物车里，不算跳过也不算新增
                continue;
            }
            if (!buyable) {
                skipped++;
                continue;
            }
            insertRelation(userId, productId, RelationTypeConstant.CART);
            added++;
        }
        result.setAdded(added);
        result.setSkipped(skipped);
        log.info("收藏批量加入购物车，用户ID：{}，新增：{}，跳过：{}", userId, added, skipped);
        return result;
    }

    /**
     * 判断关联关系是否已存在
     */
    private boolean relationExists(Long userId, Long productId, String relationType) {
        return userProductRelationMapper.selectCount(new LambdaQueryWrapper<UserProductRelation>()
                .eq(UserProductRelation::getUserId, userId)
                .eq(UserProductRelation::getProductId, productId)
                .eq(UserProductRelation::getRelationType, relationType)) > 0;
    }

    /**
     * 新增一条关联关系
     */
    private void insertRelation(Long userId, Long productId, String relationType) {
        UserProductRelation relation = new UserProductRelation();
        relation.setUserId(userId);
        relation.setProductId(productId);
        relation.setRelationType(relationType);
        userProductRelationMapper.insert(relation);
    }

    /**
     * 商品状态的中文描述，用于拼出「商品已售出，无法加入购物车」这类提示
     */
    private String statusDesc(Integer status) {
        for (ProductStatusEnum item : ProductStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return item.getDesc();
            }
        }
        return "状态异常";
    }

    /**
     * 卖家昵称：未设置昵称时回落为用户名
     */
    private String resolveNickname(User user) {
        if (user == null) {
            return "已注销用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
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
