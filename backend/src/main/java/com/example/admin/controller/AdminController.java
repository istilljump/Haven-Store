package com.example.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.dto.AdminMessageSendDTO;
import com.example.admin.dto.AdminUserCreateDTO;
import com.example.admin.dto.CategoryFormDTO;
import com.example.admin.dto.ProductStatusDTO;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.dto.UserStatusDTO;
import com.example.admin.service.AdminService;
import com.example.admin.vo.AdminMessageVO;
import com.example.admin.vo.AdminProductVO;
import com.example.admin.vo.AdminUserVO;
import com.example.admin.vo.DashboardVO;
import com.example.category.entity.Category;
import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.entity.Product;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.example.user.dto.LoginDTO;
import com.example.user.entity.User;
import com.example.user.enums.UserStatusEnum;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import com.example.user.vo.LoginUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理员后台控制器
 * <p>
 * 提供管理后台六个页面的全部接口：数据概览、用户管理、商品管理、分类管理、系统消息、系统设置。
 * <p>
 * 权限说明：/admin/** 由 {@code AdminAuthInterceptor} 统一校验管理员角色，
 * 本控制器内不再重复做权限判断，只需专注参数与业务（登录接口已在配置中放行）。
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Api(tags = "管理员后台接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 用户模块业务逻辑对象（登录复用） */
    private final UserService userService;

    /** 用户模块数据访问对象（系统统计使用） */
    private final UserMapper userMapper;

    /** 商品模块数据访问对象（系统统计使用） */
    private final ProductMapper productMapper;

    /**
     * 管理员登录
     * <p>
     * 说明：不重复实现登录逻辑，直接复用用户模块的 {@link UserService#login}，
     * 密码比对、账号状态校验、JWT 签发、Redis 缓存全部走同一套代码；
     * 本接口只额外校验角色必须为管理员，后续新增的校验规则也只需改一处。
     *
     * @param dto 登录入参（用户名、密码）
     * @return 与 /user/login 完全一致的登录信息（含 isAdmin 标识）
     */
    @ApiOperation(value = "管理员登录", notes = "复用用户登录逻辑，额外校验角色；非管理员账号拒绝登录")
    @PostMapping("/login")
    public Result<LoginUserVO> login(@RequestBody @Validated LoginDTO dto) {
        // 用户名密码校验失败会直接抛 BusinessException，无需在此重复处理
        Result<LoginUserVO> result = userService.login(dto);
        LoginUserVO loginUser = result.getData();
        // 角色校验：非管理员直接拒绝，避免普通账号进入后台
        if (loginUser == null || !Boolean.TRUE.equals(loginUser.getIsAdmin())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "该账号不是管理员，无权登录管理后台");
        }
        return result;
    }

    /**
     * 获取系统统计信息
     *
     * @return 系统统计信息
     */
    @ApiOperation(value = "获取系统统计信息", notes = "获取系统运行数据统计，包括用户数、商品数等")
    @GetMapping("/stats")
    public Result<SystemStats> getSystemStats() {
        SystemStats stats = new SystemStats();
        stats.setTotalUsers(userMapper.selectCount(null));
        stats.setActiveUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatusEnum.ENABLE.getCode())));
        stats.setTotalProducts(productMapper.selectCount(null));
        stats.setActiveProducts(productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())));
        return Result.success(stats);
    }

    // ==================== 数据概览 ====================

    /**
     * 管理后台首页数据
     *
     * @return 统计数据 + 最近商品 + 最近系统消息
     */
    @ApiOperation(value = "管理后台首页数据", notes = "返回平台统计数据、最近发布商品与最近系统消息")
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(adminService.getDashboard());
    }

    // ==================== 用户管理 ====================

    /**
     * 分页查询用户列表
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param keyword  关键词（用户名/昵称/手机号）
     * @param status   账号状态（1 正常，0 禁用）
     * @return 用户分页数据
     */
    @ApiOperation(value = "获取用户列表", notes = "管理员功能：分页查询用户，支持关键词与状态过滤")
    @GetMapping("/users")
    public Result<Page<AdminUserVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listUsers(page, pageSize, keyword, status));
    }

    /**
     * 新增用户
     *
     * @param dto 新增用户入参
     * @return 操作结果
     */
    @ApiOperation(value = "新增用户", notes = "管理员功能：后台创建账号，可指定是否为管理员")
    @PostMapping("/users")
    public Result<Void> createUser(@RequestBody @Validated AdminUserCreateDTO dto) {
        adminService.createUser(dto);
        return Result.success();
    }

    /**
     * 启用/禁用用户
     *
     * @param userId 用户 ID
     * @param dto    状态入参
     * @return 操作结果
     */
    @ApiOperation(value = "启用/禁用用户", notes = "管理员功能：切换指定用户账号状态")
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestBody @Validated UserStatusDTO dto) {
        adminService.updateUserStatus(userId, dto.getStatus());
        return Result.success();
    }

    // ==================== 商品管理 ====================

    /**
     * 分页查询商品列表
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param keyword    关键词（商品标题）
     * @param status     商品状态（1 在售，2 已售出，3 已下架）
     * @param categoryId 分类 ID
     * @return 商品分页数据
     */
    @ApiOperation(value = "获取商品列表", notes = "管理员功能：分页查询商品，支持关键词、状态、分类过滤")
    @GetMapping("/products")
    public Result<Page<AdminProductVO>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer categoryId) {
        return Result.success(adminService.listProducts(page, pageSize, keyword, status, categoryId));
    }

    /**
     * 变更商品状态（下架 / 重新上架）
     *
     * @param productId 商品 ID
     * @param dto       状态入参
     * @return 操作结果
     */
    @ApiOperation(value = "变更商品状态", notes = "管理员功能：下架或在售指定商品")
    @PutMapping("/products/{productId}/status")
    public Result<Void> updateProductStatus(@PathVariable Long productId,
                                            @RequestBody @Validated ProductStatusDTO dto) {
        adminService.updateProductStatus(productId, dto.getStatus());
        return Result.success();
    }

    // ==================== 分类管理 ====================

    /**
     * 查询全部分类
     *
     * @return 分类列表
     */
    @ApiOperation(value = "获取分类列表", notes = "管理员功能：获取全部分类，按排序值升序")
    @GetMapping("/categories")
    public Result<List<Category>> getCategories() {
        return Result.success(adminService.listCategories());
    }

    /**
     * 新增分类
     *
     * @param dto 分类表单
     * @return 操作结果
     */
    @ApiOperation(value = "新增分类", notes = "管理员功能：新增商品分类，名称全局唯一")
    @PostMapping("/categories")
    public Result<Void> addCategory(@RequestBody @Validated CategoryFormDTO dto) {
        adminService.addCategory(dto);
        return Result.success();
    }

    /**
     * 修改分类
     *
     * @param categoryId 分类 ID
     * @param dto        分类表单
     * @return 操作结果
     */
    @ApiOperation(value = "修改分类", notes = "管理员功能：修改分类名称、排序与状态")
    @PutMapping("/categories/{categoryId}")
    public Result<Void> updateCategory(@PathVariable Integer categoryId,
                                       @RequestBody @Validated CategoryFormDTO dto) {
        adminService.updateCategory(categoryId, dto);
        return Result.success();
    }

    /**
     * 删除分类
     *
     * @param categoryId 分类 ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除分类", notes = "管理员功能：删除分类；分类下存在商品时拒绝删除")
    @DeleteMapping("/categories/{categoryId}")
    public Result<Void> deleteCategory(@PathVariable Integer categoryId) {
        adminService.deleteCategory(categoryId);
        return Result.success();
    }

    // ==================== 系统消息 ====================

    /**
     * 分页查询系统消息
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param userType 接收群体（all/admin/user）
     * @param status   消息状态（1 发送中，2 已送达）
     * @return 消息分页数据
     */
    @ApiOperation(value = "获取系统消息列表", notes = "管理员功能：按公告聚合分页查询已发送的系统消息")
    @GetMapping("/messages")
    public Result<Page<AdminMessageVO>> getMessages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listMessages(page, pageSize, userType, status));
    }

    /**
     * 群发系统消息
     *
     * @param dto 群发入参
     * @return 操作结果
     */
    @ApiOperation(value = "发送系统消息", notes = "管理员功能：按接收群体群发系统消息")
    @PostMapping("/messages")
    public Result<Void> sendMessage(@RequestBody @Validated AdminMessageSendDTO dto) {
        adminService.sendMessage(dto);
        return Result.success();
    }

    // ==================== 系统设置 ====================

    /**
     * 获取系统设置
     *
     * @return 系统设置
     */
    @ApiOperation(value = "获取系统设置", notes = "管理员功能：读取站点、上传、交易、联系、安全等配置")
    @GetMapping("/settings")
    public Result<SystemSettingDTO> getSettings() {
        return Result.success(adminService.getSettings());
    }

    /**
     * 保存系统设置
     *
     * @param dto 系统设置
     * @return 操作结果
     */
    @ApiOperation(value = "保存系统设置", notes = "管理员功能：保存站点、上传、交易、联系、安全等配置")
    @PutMapping("/settings")
    public Result<Void> updateSettings(@RequestBody SystemSettingDTO dto) {
        adminService.updateSettings(dto);
        return Result.success();
    }

    // ==================== 数据导出 ====================

    /**
     * 导出用户数据（CSV）
     * <p>
     * 返回带 UTF-8 BOM 的 CSV，保证 Excel 直接打开时中文不乱码
     *
     * @param keyword 关键词（用户名/昵称/手机号）
     * @param status  账号状态（1 正常，0 禁用）
     * @return CSV 文件流
     */
    @ApiOperation(value = "导出用户数据", notes = "管理员功能：按当前筛选条件导出用户数据为 CSV")
    @GetMapping("/export/users")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        String csv = adminService.exportUsersCsv(keyword, status);
        return buildCsvResponse(csv, "users.csv");
    }

    /**
     * 导出商品数据（CSV）
     *
     * @param keyword    关键词（商品标题）
     * @param status     商品状态
     * @param categoryId 分类 ID
     * @return CSV 文件流
     */
    @ApiOperation(value = "导出商品数据", notes = "管理员功能：按当前筛选条件导出商品数据为 CSV")
    @GetMapping("/export/products")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer categoryId) {
        String csv = adminService.exportProductsCsv(keyword, status, categoryId);
        return buildCsvResponse(csv, "products.csv");
    }

    /**
     * 构建 CSV 下载响应
     *
     * @param csv       CSV 文本
     * @param fileName  下载文件名（ASCII，前端会按需覆盖）
     * @return 带下载响应头的响应实体
     */
    private ResponseEntity<byte[]> buildCsvResponse(String csv, String fileName) {
        // U+FEFF 为 UTF-8 BOM，Excel 依赖它识别编码
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(body.length);
        return new ResponseEntity<>(body, headers, org.springframework.http.HttpStatus.OK);
    }

    // ==================== 内部数据类 ====================

    /**
     * 系统统计信息数据类
     */
    public static class SystemStats {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalProducts;
        private Long activeProducts;

        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { this.activeUsers = activeUsers; }
        public Long getTotalProducts() { return totalProducts; }
        public void setTotalProducts(Long totalProducts) { this.totalProducts = totalProducts; }
        public Long getActiveProducts() { return activeProducts; }
        public void setActiveProducts(Long activeProducts) { this.activeProducts = activeProducts; }
    }
}
