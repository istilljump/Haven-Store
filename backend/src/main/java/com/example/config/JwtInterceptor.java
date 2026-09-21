package com.example.config;

import com.example.common.Constants;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.utils.JwtUtil;
import com.example.utils.UserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 登录鉴权拦截器
 * <p>
 * 请求前置阶段从请求头解析 Token 并校验，通过后将用户 ID 写入 ThreadLocal（UserHolder），
 * Token 缺失或无效时直接写出 401 业务码的统一 Result 响应；
 * 请求结束后在 afterCompletion 中清理 ThreadLocal，防止线程池复用导致内存泄漏
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    /** JWT 工具类 */
    private final JwtUtil jwtUtil;

    /** Jackson 序列化工具，用于向客户端写出 JSON 响应 */
    private final ObjectMapper objectMapper;

    /** 存放 Token 的请求头名称（从配置文件读取） */
    @Value("${jwt.header}")
    private String header;

    /**
     * 请求前置处理：校验 Token，校验通过后将用户 ID 写入当前线程上下文
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  目标处理器
     * @return true 放行；false 拦截（已写出 401 响应）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // 1. 从请求头获取 Token
        String token = request.getHeader(header);
        if (!StringUtils.hasText(token)) {
            log.warn("请求未携带 Token，URI：{}", request.getRequestURI());
            writeUnauthorized(response);
            return false;
        }
        // 2. 去除 Bearer 前缀（兼容直接传裸 Token 的客户端）
        if (token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.substring(Constants.TOKEN_PREFIX.length());
        }
        // 3. 校验 Token 有效性（签名正确且未过期）
        if (!jwtUtil.validateToken(token)) {
            log.warn("请求 Token 校验失败，URI：{}", request.getRequestURI());
            writeUnauthorized(response);
            return false;
        }
        // 4. 解析用户 ID 并存入线程上下文，供后续业务直接获取
        Long userId = jwtUtil.getUserIdFromToken(token);
        UserHolder.setUserId(userId);
        return true;
    }

    /**
     * 请求完成后清理 ThreadLocal，防止线程池复用导致的用户数据串用与内存泄漏
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  目标处理器
     * @param ex       处理过程中抛出的异常（可能为 null）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.remove();
    }

    /**
     * 向客户端写出未登录响应
     * <p>
     * 说明：HTTP 状态码保持 200，业务状态码 401 标识未登录，
     * 前端依据响应体中的 code 字段统一处理跳转登录页
     *
     * @param response 当前响应
     */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ResultCodeEnum.UNAUTHORIZED)));
    }
}
