package com.example.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员后台控制器
 * <p>
 * 提供系统管理功能接口，包括用户管理、系统配置等
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Api(tags = "管理员后台接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    /** 用户模块业务逻辑对象 */
    private final UserService userService;

    /** 用户模块数据访问对象（用于系统统计） */
    private final UserMapper userMapper;

    /** 商品模块数据访问对象（用于系统统计） */
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

        // 用户统计：总数 + 状态正常的用户数
        stats.setTotalUsers(userMapper.selectCount(null));
        stats.setActiveUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatusEnum.ENABLE.getCode())));
        // 商品统计：总数 + 上架中的商品数
        stats.setTotalProducts(productMapper.selectCount(null));
        stats.setActiveProducts(productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())));

        return Result.success(stats);
    }

    /**
     * 获取用户列表（管理员功能）
     *
     * @param page 页码
     * @param size 每页大小
     * @return 用户列表
     */
    @ApiOperation(value = "获取用户列表", notes = "管理员功能：获取系统所有用户列表，支持分页")
    @GetMapping("/users")
    public Result<Page<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // TODO: 实现用户列表查询逻辑
        return Result.success(new Page<>());
    }

    /**
     * 禁用/启用用户
     *
     * @param userId 用户ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    @ApiOperation(value = "禁用/启用用户", notes = "管理员功能：禁用或启用指定用户账户")
    @PostMapping("/users/{userId}/toggle")
    public Result<Void> toggleUser(@PathVariable Long userId, @RequestParam Boolean enabled) {
        // TODO: 实现用户禁用/启用逻辑
        return Result.success();
    }

    /**
     * 重置用户密码
     *
     * @param userId 用户ID
     * @return 新密码
     */
    @ApiOperation(value = "重置用户密码", notes = "管理员功能：重置指定用户密码，返回新密码")
    @PostMapping("/users/{userId}/reset-password")
    public Result<String> resetUserPassword(@PathVariable Long userId) {
        // TODO: 实现用户密码重置逻辑
        return Result.success("new_password_123");
    }

    /**
     * 系统配置更新
     *
     * @param config 配置信息
     * @return 操作结果
     */
    @ApiOperation(value = "更新系统配置", notes = "管理员功能：更新系统配置参数")
    @PostMapping("/config")
    public Result<Void> updateSystemConfig(@RequestBody SystemConfig config) {
        // TODO: 实现系统配置更新逻辑
        return Result.success();
    }

    /**
     * 获取系统日志
     *
     * @param page 页码
     * @param size 每页大小
     * @return 日志列表
     */
    @ApiOperation(value = "获取系统日志", notes = "管理员功能：获取系统运行日志")
    @GetMapping("/logs")
    public Result<SystemLogs> getSystemLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // TODO: 实现系统日志查询逻辑
        return Result.success(new SystemLogs());
    }

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

    /**
     * 系统配置数据类
     */
    public static class SystemConfig {
        private Boolean allowRegister;
        private String siteName;
        private String adminEmail;

        // Getters and Setters
        public Boolean getAllowRegister() { return allowRegister; }
        public void setAllowRegister(Boolean allowRegister) { this.allowRegister = allowRegister; }
        public String getSiteName() { return siteName; }
        public void setSiteName(String siteName) { this.siteName = siteName; }
        public String getAdminEmail() { return adminEmail; }
        public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    }

    /**
     * 系统日志数据类
     */
    public static class SystemLogs {
        private List<String> logs;

        // Getters and Setters
        public List<String> getLogs() { return logs; }
        public void setLogs(List<String> logs) { this.logs = logs; }
    }
}