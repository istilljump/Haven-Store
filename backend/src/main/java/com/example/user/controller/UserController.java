package com.example.user.controller;

import com.example.common.Result;
import com.example.user.dto.ChangePasswordDTO;
import com.example.user.dto.LoginDTO;
import com.example.user.dto.RegisterDTO;
import com.example.user.dto.UserUpdateDTO;
import com.example.user.service.UserService;
import com.example.user.vo.LoginUserVO;
import com.example.user.vo.UserInfoVO;
import com.example.utils.FileStorageUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /** 图片存储工具（头像上传） */
    private final FileStorageUtil fileStorageUtil;

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
    public Result<UserInfoVO> getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }

    /**
     * 修改当前登录用户的资料
     *
     * @param dto 资料入参（昵称、手机号、邮箱、个人简介）
     * @return 操作结果
     */
    @ApiOperation(value = "修改用户资料", notes = "需要登录态；用户名不可修改")
    @PutMapping("/info")
    public Result<Void> updateCurrentUserInfo(@RequestBody @Validated UserUpdateDTO dto) {
        userService.updateCurrentUserInfo(dto);
        return Result.success();
    }

    /**
     * 修改当前登录用户的密码
     *
     * @param dto 改密入参（原密码、新密码、确认新密码）
     * @return 操作结果
     */
    @ApiOperation(value = "修改密码", notes = "需要登录态；会校验原密码与两次新密码一致性")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }

    /**
     * 上传并更新当前登录用户的头像
     *
     * @param file 头像图片
     * @return 新的头像地址
     */
    @ApiOperation(value = "上传头像", notes = "需要登录态；仅支持 jpg/jpeg/png/gif/webp，单张不超过 5MB")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileStorageUtil.storeImage(file, "avatar");
        userService.updateAvatar(url);
        return Result.success(url);
    }
}
