package com.example.user.service;

import com.example.common.Result;
import com.example.user.dto.LoginDTO;
import com.example.user.dto.ChangePasswordDTO;
import com.example.user.dto.RegisterDTO;
import com.example.user.dto.UserUpdateDTO;
import com.example.user.entity.User;
import com.example.user.vo.LoginUserVO;
import com.example.user.vo.UserInfoVO;

/**
 * 用户模块业务逻辑接口
 *
 * @author ZCode
 * @date 2026/09/21
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param dto 注册入参
     * @return 注册结果
     */
    Result<Void> register(RegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录入参
     * @return 登录用户信息（含 JWT Token）
     */
    Result<LoginUserVO> login(LoginDTO dto);

    /**
     * 获取当前登录用户信息（脱敏）
     *
     * @return 当前登录用户信息（不含密码字段）
     */
    Result<UserInfoVO> getCurrentUserInfo();

    /**
     * 修改当前登录用户的资料
     * <p>
     * 用户名不可修改；手机号填写时需要保证全局唯一
     *
     * @param dto 资料入参
     */
    void updateCurrentUserInfo(UserUpdateDTO dto);

    /**
     * 修改当前登录用户的密码
     *
     * @param dto 改密入参（原密码、新密码、确认新密码）
     */
    void changePassword(ChangePasswordDTO dto);

    /**
     * 更新当前登录用户的头像
     *
     * @param avatarUrl 头像地址（由上传接口返回）
     */
    void updateAvatar(String avatarUrl);

    /**
     * 加载用于鉴权的用户实体（Redis 缓存优先，未命中回源数据库并重建缓存）
     * <p>
     * 供 JWT 拦截器在每个请求上校验账号状态（禁用账号的 Token 立即失效），
     * 密码字段不参与缓存
     *
     * @param userId 用户 ID
     * @return 用户实体；用户不存在时返回 null
     */
    User getAuthUser(Long userId);
}
