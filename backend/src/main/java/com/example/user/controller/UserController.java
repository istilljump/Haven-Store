package com.example.user.controller;

import com.example.common.Result;
import com.example.user.dto.LoginDTO;
import com.example.user.dto.RegisterDTO;
import com.example.user.entity.User;
import com.example.user.service.UserService;
import com.example.user.vo.LoginUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如注册接口：POST /api/user/register
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Api(tags = "用户模块接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    /** 用户模块业务逻辑对象 */
    private final UserService userService;

    /**
     * 用户注册
     *
     * @param dto 注册入参（用户名、密码、确认密码、手机号、昵称）
     * @return 注册结果
     */
    @ApiOperation(value = "用户注册", notes = "用户名与手机号全局唯一，密码 BCrypt 加密存储")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated RegisterDTO dto) {
        return userService.register(dto);
    }

    /**
     * 用户登录
     *
     * @param dto 登录入参（用户名、密码）
     * @return 登录用户信息与 JWT Token
     */
    @ApiOperation(value = "用户登录", notes = "登录成功返回 Token，后续请求携带请求头：Authorization: Bearer {token}")
    @PostMapping("/login")
    public Result<LoginUserVO> login(@RequestBody @Validated LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * 获取当前登录用户信息（需要登录态）
     *
     * @return 当前登录用户信息（已脱敏，不含密码）
     */
    @ApiOperation(value = "获取当前登录用户信息", notes = "需要登录态，返回脱敏后的用户信息")
    @GetMapping("/info")
    public Result<User> getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }
}
