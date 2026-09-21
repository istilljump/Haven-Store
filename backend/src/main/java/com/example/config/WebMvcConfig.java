package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SpringMVC 配置类：注册 JWT 鉴权拦截器
 * <p>
 * 默认拦截所有请求，登录、注册、接口文档等白名单路径放行
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** JWT 鉴权拦截器 */
    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册拦截器并配置放行路径
     * <p>
     * 说明：路径匹配是相对于上下文路径 /api 之后的路径
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 用户模块：登录、注册接口放行
                        "/user/login",
                        "/user/register",
                        // 系统默认错误跳转路径放行，避免异常信息被鉴权拦截器覆盖
                        "/error",
                        // Knife4j 接口文档页面与静态资源放行
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/v2/api-docs/**",
                        "/favicon.ico"
                );
    }
}
