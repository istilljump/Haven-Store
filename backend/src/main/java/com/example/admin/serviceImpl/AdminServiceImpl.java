package com.example.admin.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.dto.AdminMessageSendDTO;
import com.example.admin.dto.AdminUserCreateDTO;
import com.example.admin.dto.CategoryFormDTO;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.AdminService;
import com.example.admin.vo.AdminMessageVO;
import com.example.admin.vo.AdminProductVO;
import com.example.admin.vo.AdminUserVO;
import com.example.admin.vo.DashboardVO;
import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.common.BusinessException;
import com.example.common.RedisKeyConst;
import com.example.message.entity.Message;
import com.example.message.enums.MessageReceiverTypeEnum;
import com.example.message.mapper.MessageMapper;
import com.example.product.entity.Product;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.example.user.constant.UserConstant;
import com.example.user.entity.User;
import com.example.user.enums.UserStatusEnum;
import com.example.user.mapper.UserMapper;
import com.example.utils.PasswordUtil;
import com.example.utils.RedisUtil;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台业务逻辑实现类
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    /** 消息状态：发送中（群发尚未完成，当前为同步发送，仅用于筛选条件兼容） */
    private static final int MESSAGE_STATUS_SENDING = 1;

    /** 消息状态：已送达（接收方记录已全部落库） */
    private static final int MESSAGE_STATUS_DELIVERED = 2;

    /** 管理后台首页「最近商品」展示条数 */
    private static final int DASHBOARD_RECENT_LIMIT = 5;

    /** 分类启用状态 */
    private static final int CATEGORY_STATUS_ENABLE = 1;

    /** 分类默认状态值（前端表单未传时使用） */
    private static final int CATEGORY_DEFAULT_SORT = 99;

    private final UserMapper userMapper;

    private final ProductMapper productMapper;

    private final CategoryMapper categoryMapper;

    private final MessageMapper messageMapper;

    private final PasswordUtil passwordUtil;

    private final RedisUtil redisUtil;

    // ==================== 用户管理 ====================

    /**
     * 分页查询用户列表
     * <p>
     * 关键词对用户名、昵称、手机号做 OR 模糊匹配，用 and(...) 包裹避免与状态条件之间的优先级错误
     */
    @Override
    public Page<AdminUserVO> listUsers(Integer page, Integer pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(User::getUsername, kw)
                    .or().like(User::getNickname, kw)
                    .or().like(User::getPhone, kw));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        // 新注册的排在前面
        wrapper.orderByDesc(User::getCreateTime).orderByDesc(User::getId);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Page<AdminUserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream().map(this::toUserVO).collect(Collectors.toList()));
        return result;
    }

    /**
     * 新增用户
     * <p>
     * 校验用户名（必填唯一）与手机号（可选，填写时唯一），密码走与前台注册同一套 BCrypt 加密
     */
    @Override
    public void createUser(AdminUserCreateDTO dto) {
        // 1. 用户名唯一
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException("用户名已存在");
        }
        // 2. 手机号唯一（允许留空）
        boolean hasPhone = StringUtils.hasText(dto.getPhone());
        if (hasPhone) {
            Long phoneCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
            if (phoneCount != null && phoneCount > 0) {
                throw new BusinessException("该手机号已被使用");
            }
        }
        // 3. 组装实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setPhone(hasPhone ? dto.getPhone() : null);
        // 昵称留空时用用户名兜底，避免列表页出现空白
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setAvatar(UserConstant.DEFAULT_AVATAR);
        user.setStatus(UserStatusEnum.ENABLE.getCode());
        user.setRole(Boolean.TRUE.equals(dto.getIsAdmin()) ? UserConstant.ROLE_ADMIN : UserConstant.ROLE_USER);
        userMapper.insert(user);

        log.info("管理员新增用户成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
    }

    /**
     * 启用/禁用用户
     * <p>
     * 拒绝管理员禁用自己：否则会把当前登录的管理员锁在系统外
     */
    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        boolean validStatus = UserStatusEnum.ENABLE.getCode().equals(status)
                || UserStatusEnum.DISABLE.getCode().equals(status);
        if (!validStatus) {
            throw new BusinessException("非法的用户状态");
        }
        Long currentUserId = UserHolder.getUserId();
        if (UserStatusEnum.DISABLE.getCode().equals(status) && userId.equals(currentUserId)) {
            throw new BusinessException("不能禁用当前登录的账号");
        }

        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);

        // 账号被禁用后需要立即失效：清除其登录态缓存，避免仍凭旧 Token 访问
        if (UserStatusEnum.DISABLE.getCode().equals(status)) {
            redisUtil.delete(RedisKeyConst.USER_INFO_KEY + userId);
        }
        log.info("管理员变更用户状态，用户ID：{}，新状态：{}", userId, status);
    }

    // ==================== 数据概览 ====================

    /**
     * 管理后台首页数据
     */
    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        DashboardVO.Statistics statistics = vo.getStatistics();

        statistics.setTotalUsers(userMapper.selectCount(null));
        statistics.setActiveUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatusEnum.ENABLE.getCode())));
        statistics.setTotalProducts(productMapper.selectCount(null));
        statistics.setOnShelfProducts(productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())));
        statistics.setTotalCategories(categoryMapper.selectCount(null));
        // 今日新增：按当天 00:00:00 起的发布时间统计
        statistics.setTodayProducts(productMapper.selectCount(
                new LambdaQueryWrapper<Product>().ge(Product::getCreateTime, LocalDate.now().atStartOfDay())));

        // 最近发布的商品
        List<Product> recentProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .orderByDesc(Product::getCreateTime)
                        .orderByDesc(Product::getId)
                        .last("LIMIT " + DASHBOARD_RECENT_LIMIT));
        vo.setRecentProducts(recentProducts.stream().map(p -> {
            DashboardVO.RecentProduct item = new DashboardVO.RecentProduct();
            item.setId(p.getId());
            item.setTitle(p.getTitle());
            item.setPrice(p.getPrice());
            item.setStatus(p.getStatus());
            item.setCreateTime(p.getCreateTime());
            return item;
        }).collect(Collectors.toList()));

        // 最近发送的系统消息（按公告聚合后取前几条）
        List<AdminMessageVO> announcements = aggregateAnnouncements(null);
        vo.setSystemMessages(announcements.stream().limit(DASHBOARD_RECENT_LIMIT).map(a -> {
            DashboardVO.RecentMessage item = new DashboardVO.RecentMessage();
            item.setId(a.getId());
            item.setTitle(a.getTitle());
            item.setCreateTime(a.getCreateTime());
            return item;
        }).collect(Collectors.toList()));

        return vo;
    }

    // ==================== 商品管理 ====================

    /**
     * 分页查询商品列表
     * <p>
     * 列表需要展示「发布者」与「分类名称」，这里按 ID 批量回查后填充，
     * 避免 N+1 查询，也不需要为管理端单独写联表 SQL
     */
    @Override
    public Page<AdminProductVO> listProducts(Integer page, Integer pageSize, String keyword,
                                             Integer status, Integer categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getTitle, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);

        Page<Product> productPage = productMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<Product> records = productPage.getRecords();

        Map<Long, String> usernameMap = loadUsernameMap(records);
        Map<Integer, String> categoryNameMap = loadCategoryNameMap();

        Page<AdminProductVO> result = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        result.setRecords(records.stream()
                .map(p -> toProductVO(p, usernameMap, categoryNameMap))
                .collect(Collectors.toList()));
        return result;
    }

    /**
     * 变更商品状态（下架 / 重新上架）
     */
    @Override
    public void updateProductStatus(Long productId, Integer status) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        boolean valid = false;
        for (ProductStatusEnum item : ProductStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BusinessException("非法的商品状态");
        }

        Product update = new Product();
        update.setId(productId);
        update.setStatus(status);
        productMapper.updateById(update);
        log.info("管理员变更商品状态，商品ID：{}，新状态：{}", productId, status);
    }

    /**
     * 删除商品（物理删除）
     */
    @Override
    public void deleteProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.deleteById(productId);
        log.info("管理员删除商品，商品ID：{}，标题：{}", productId, product.getTitle());
    }

    // ==================== 分类管理 ====================

    @Override
    public List<Category> listCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
    }

    @Override
    public void addCategory(CategoryFormDTO dto) {
        Long nameCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getName, dto.getName()));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException("分类名称已存在");
        }
        Category category = new Category();
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? CATEGORY_DEFAULT_SORT : dto.getSort());
        category.setStatus(dto.getStatus() == null ? CATEGORY_STATUS_ENABLE : dto.getStatus());
        categoryMapper.insert(category);
        log.info("新增商品分类，分类ID：{}，名称：{}", category.getId(), category.getName());
    }

    @Override
    public void updateCategory(Integer categoryId, CategoryFormDTO dto) {
        Category exist = categoryMapper.selectById(categoryId);
        if (exist == null) {
            throw new BusinessException("分类不存在");
        }
        // 名称唯一性校验时排除自身
        Long nameCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getName, dto.getName())
                        .ne(Category::getId, categoryId));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException("分类名称已存在");
        }
        Category update = new Category();
        update.setId(categoryId);
        update.setName(dto.getName());
        update.setSort(dto.getSort() == null ? exist.getSort() : dto.getSort());
        update.setStatus(dto.getStatus() == null ? exist.getStatus() : dto.getStatus());
        categoryMapper.updateById(update);
        log.info("修改商品分类，分类ID：{}，名称：{}", categoryId, dto.getName());
    }

    /**
     * 删除分类
     * <p>
     * 分类下仍有商品时拒绝删除，避免商品表留下悬空的 category_id
     */
    @Override
    public void deleteCategory(Integer categoryId) {
        Category exist = categoryMapper.selectById(categoryId);
        if (exist == null) {
            throw new BusinessException("分类不存在");
        }
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, categoryId));
        if (productCount != null && productCount > 0) {
            throw new BusinessException("该分类下存在 " + productCount + " 件商品，无法删除");
        }
        categoryMapper.deleteById(categoryId);
        log.info("删除商品分类，分类ID：{}，名称：{}", categoryId, exist.getName());
    }

    // ==================== 系统消息 ====================

    /**
     * 分页查询系统消息
     * <p>
     * 群发时一条公告会按接收者展开成多行，这里按「标题 + 内容 + 接收群体 + 消息类型」聚合，
     * 让管理后台看到的是一条公告而不是 N 条重复记录
     */
    @Override
    public Page<AdminMessageVO> listMessages(Integer page, Integer pageSize, String userType, Integer status) {
        // 同步群发，落库即送达；筛选「发送中」时结果必然为空
        if (status != null && status == MESSAGE_STATUS_SENDING) {
            return new Page<>(page, pageSize, 0);
        }

        List<AdminMessageVO> all = aggregateAnnouncements(userType);

        // 分组已完成，分页在内存中完成（消息量级小，且分组后无法用 SQL LIMIT 直接表达）
        long total = all.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(all.size(), fromIndex + pageSize);
        List<AdminMessageVO> pageRecords = fromIndex >= all.size()
                ? Collections.emptyList()
                : new ArrayList<>(all.subList(fromIndex, toIndex));

        Page<AdminMessageVO> result = new Page<>(page, pageSize, total);
        result.setRecords(pageRecords);
        return result;
    }

    /**
     * 群发系统消息
     * <p>
     * 接收范围由 userType 决定，命中几个用户就落几条记录（消息表按接收者存储，便于各自维护已读状态）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(AdminMessageSendDTO dto) {
        MessageReceiverTypeEnum receiverType = MessageReceiverTypeEnum.of(dto.getUserType());
        if (receiverType == null) {
            throw new BusinessException("接收用户类型不合法");
        }

        List<User> receivers = resolveReceivers(receiverType);
        if (receivers.isEmpty()) {
            throw new BusinessException("当前没有符合条件的接收用户");
        }

        for (User receiver : receivers) {
            Message message = new Message();
            message.setReceiverId(receiver.getId());
            message.setMessageType(receiverType.getMessageType());
            message.setReceiverType(receiverType.getCode());
            message.setTitle(dto.getTitle());
            message.setContent(dto.getContent());
            // 0 未读
            message.setIsRead(0);
            messageMapper.insert(message);
        }
        log.info("管理员群发系统消息，接收范围：{}，送达人数：{}，标题：{}",
                receiverType.getDesc(), receivers.size(), dto.getTitle());
    }

    // ==================== 系统设置 ====================

    @Override
    public SystemSettingDTO getSettings() {
        SystemSettingDTO cached = redisUtil.get(RedisKeyConst.SYSTEM_SETTINGS_KEY);
        return cached == null ? SystemSettingDTO.defaults() : cached;
    }

    @Override
    public void updateSettings(SystemSettingDTO dto) {
        if (dto == null) {
            throw new BusinessException("设置内容不能为空");
        }
        // 站点名称是前台页面标题的数据源，不允许留空
        if (dto.getSite() == null || !StringUtils.hasText(dto.getSite().getName())) {
            throw new BusinessException("站点名称不能为空");
        }
        redisUtil.set(RedisKeyConst.SYSTEM_SETTINGS_KEY, dto);
        log.info("管理员更新系统设置，站点名称：{}", dto.getSite().getName());
    }

    // ==================== 数据导出 ====================

    @Override
    public String exportUsersCsv(String keyword, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(User::getUsername, kw)
                    .or().like(User::getNickname, kw)
                    .or().like(User::getPhone, kw));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime).orderByDesc(User::getId);

        StringBuilder sb = new StringBuilder();
        sb.append("用户ID,用户名,昵称,手机号,角色,状态,注册时间\n");
        for (User user : userMapper.selectList(wrapper)) {
            sb.append(csvCell(user.getId())).append(',')
                    .append(csvCell(user.getUsername())).append(',')
                    .append(csvCell(user.getNickname())).append(',')
                    .append(csvCell(user.getPhone())).append(',')
                    .append(csvCell(UserConstant.ROLE_ADMIN.equals(user.getRole()) ? "管理员" : "普通用户")).append(',')
                    .append(csvCell(UserStatusEnum.ENABLE.getCode().equals(user.getStatus()) ? "正常" : "禁用")).append(',')
                    .append(csvCell(user.getCreateTime()))
                    .append('\n');
        }
        return sb.toString();
    }

    @Override
    public String exportProductsCsv(String keyword, Integer status, Integer categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getTitle, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);

        List<Product> products = productMapper.selectList(wrapper);
        Map<Long, String> usernameMap = loadUsernameMap(products);
        Map<Integer, String> categoryNameMap = loadCategoryNameMap();

        StringBuilder sb = new StringBuilder();
        sb.append("商品ID,商品标题,发布者,分类,价格,成色,交易方式,状态,发布时间\n");
        for (Product product : products) {
            sb.append(csvCell(product.getId())).append(',')
                    .append(csvCell(product.getTitle())).append(',')
                    .append(csvCell(usernameMap.get(product.getUserId()))).append(',')
                    .append(csvCell(categoryNameMap.get(product.getCategoryId()))).append(',')
                    .append(csvCell(product.getPrice())).append(',')
                    .append(csvCell(product.getProductCondition())).append(',')
                    .append(csvCell(product.getTradeType())).append(',')
                    .append(csvCell(statusText(product.getStatus()))).append(',')
                    .append(csvCell(product.getCreateTime()))
                    .append('\n');
        }
        return sb.toString();
    }

    // ==================== 私有工具方法 ====================

    /**
     * 按公告聚合系统消息
     *
     * @param userType 接收群体过滤条件，为空表示不限
     * @return 按发送时间倒序排列的公告列表
     */
    private List<AdminMessageVO> aggregateAnnouncements(String userType) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        // 仅聚合管理后台群发产生的记录（带 receiver_type 标记）
        wrapper.isNotNull(Message::getReceiverType);
        if (StringUtils.hasText(userType)) {
            wrapper.eq(Message::getReceiverType, userType);
        }
        wrapper.orderByDesc(Message::getCreateTime).orderByDesc(Message::getId);

        List<Message> messages = messageMapper.selectList(wrapper);

        // key 相同的记录属于同一次群发，用 LinkedHashMap 保持时间倒序
        Map<String, AdminMessageVO> grouped = new LinkedHashMap<>();
        for (Message message : messages) {
            String key = message.getTitle() + '\u0001' + message.getContent() + '\u0001'
                    + message.getReceiverType() + '\u0001' + message.getMessageType();
            AdminMessageVO existing = grouped.get(key);
            if (existing == null) {
                AdminMessageVO item = new AdminMessageVO();
                item.setId(message.getId());
                item.setTitle(message.getTitle());
                item.setContent(message.getContent());
                item.setMessageType(message.getMessageType());
                item.setUserType(message.getReceiverType());
                item.setCreateTime(message.getCreateTime());
                item.setStatus(MESSAGE_STATUS_DELIVERED);
                item.setReceiverCount(1);
                grouped.put(key, item);
            } else {
                existing.setReceiverCount(existing.getReceiverCount() + 1);
                // 同组中取更早的创建时间，使发送时间更贴近真实
                if (message.getCreateTime() != null
                        && (existing.getCreateTime() == null
                        || message.getCreateTime().isBefore(existing.getCreateTime()))) {
                    existing.setCreateTime(message.getCreateTime());
                    existing.setId(message.getId());
                }
            }
        }
        return new ArrayList<>(grouped.values());
    }

    /**
     * 根据接收群体解析目标用户列表
     */
    private List<User> resolveReceivers(MessageReceiverTypeEnum receiverType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        switch (receiverType) {
            case ADMIN:
                wrapper.eq(User::getRole, UserConstant.ROLE_ADMIN);
                break;
            case USER:
                wrapper.eq(User::getRole, UserConstant.ROLE_USER);
                break;
            case ALL:
            default:
                // 所有用户：无额外条件
                break;
        }
        // 不给已禁用的账号发消息
        wrapper.eq(User::getStatus, UserStatusEnum.ENABLE.getCode());
        return userMapper.selectList(wrapper);
    }

    /**
     * 批量查询商品发布者的用户名
     */
    private Map<Long, String> loadUsernameMap(List<Product> products) {
        List<Long> userIds = products.stream()
                .map(Product::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    /**
     * 查询分类 ID -> 分类名称映射
     */
    private Map<Integer, String> loadCategoryNameMap() {
        List<Category> categories = categoryMapper.selectList(null);
        if (categories.isEmpty()) {
            return Collections.emptyMap();
        }
        return categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private AdminUserVO toUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setIsAdmin(UserConstant.ROLE_ADMIN.equals(user.getRole()));
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    private AdminProductVO toProductVO(Product product, Map<Long, String> usernameMap,
                                       Map<Integer, String> categoryNameMap) {
        AdminProductVO vo = new AdminProductVO();
        vo.setId(product.getId());
        vo.setTitle(product.getTitle());
        vo.setDescription(product.getDescription());
        vo.setCoverImage(product.getCoverImage());
        vo.setUserId(product.getUserId());
        vo.setUsername(usernameMap.get(product.getUserId()));
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryNameMap.get(product.getCategoryId()));
        vo.setPrice(product.getPrice());
        vo.setProductCondition(product.getProductCondition());
        vo.setTradeType(product.getTradeType());
        vo.setAddress(product.getAddress());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private String statusText(Integer status) {
        for (ProductStatusEnum item : ProductStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return item.getDesc();
            }
        }
        return "未知";
    }

    /**
     * CSV 单元格转义
     * <p>
     * 逗号、引号、换行都会破坏 CSV 结构，统一用双引号包裹并把内部引号转义为两个引号
     */
    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
