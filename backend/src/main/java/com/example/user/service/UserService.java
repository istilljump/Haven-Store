package com.example.user.service;

import com.example.common.Result;
import com.example.user.dto.LoginDTO;
import com.example.user.dto.RegisterDTO;
import com.example.user.entity.User;
import com.example.user.vo.LoginUserVO;

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
    Result<User> getCurrentUserInfo();
}
