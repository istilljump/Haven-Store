package com.example.admin.interceptor;

import com.example.common.BusinessException;
import com.example.common.ResultCodeEnum;
import com.example.user.constant.UserConstant;
import com.example.user.entity.User;
import com.example.user.enums.UserStatusEnum;
import com.example.user.mapper.UserMapper;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理后台统一鉴权拦截器
 * <p>
 * 说明：JWT 拦截器先完成登录态解析并把用户 ID 写入 {@link UserHolder}，
 * 本拦截器在其后执行，只做一件事——校验当前登录用户角色是否为管理员。
 * 统一在拦截器层做校验的好处：/admin/** 下新增任何接口自动受保护，
 * 无需在每个 Controller 方法里重复编写权限判断（复用一处，全局生效）。
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    /** 用户模块数据访问对象（用于回查角色） */
    private final UserMapper userMapper;

    /**
     * 管理接口权限校验：
     * 未登录返回 401；账号不存在或被禁用返回 401；非管理员返回 403
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 登录态兜底校验（正常情况下 JWT 拦截器已挡住未登录请求）
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        // 2. 回查用户，账号不存在或被禁用一律视为未登录
        User user = userMapper.selectById(userId);
        if (user == null || !UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        // 3. 角色校验：非管理员拒绝访问管理接口
        if (!UserConstant.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "该账号不是管理员，无权访问管理接口");
        }
        return true;
    }
}
