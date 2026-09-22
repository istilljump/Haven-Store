package com.example.config;

import com.example.admin.interceptor.AdminAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SpringMVC 配置类：全局跨域配置 + JWT 鉴权拦截器注册
 * <p>
 * 跨域：前后端分离架构下放行前端来源（来源列表由配置文件管理，生产环境收敛为具体域名）；
 * 拦截器：默认拦截所有请求，登录、注册、接口文档等白名单路径放行
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** JWT 鉴权拦截器 */
    private final JwtInterceptor jwtInterceptor;

    /** 管理后台权限拦截器：/admin/** 下接口统一校验管理员角色 */
    private final AdminAuthInterceptor adminAuthInterceptor;

    /** 允许跨域的前端来源（从配置文件读取，支持配置多个，逗号分隔） */
    @Value("${cors.origin-patterns}")
    private String[] corsOriginPatterns;

    /** 上传文件的落盘目录（与 UploadController 使用同一配置项） */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 静态资源映射：把上传目录暴露为 /uploads/**
     * <p>
     * 说明：图片是通过 img 标签直接访问的，请求不会带上 Authorization 头，
     * 因此 /uploads/** 已在下面的拦截器白名单中放行
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    /**
     * 全局跨域配置
     * <p>
     * 说明：预检（OPTIONS）请求由 Spring CORS 处理器接管，不经过业务拦截器；
     * allowCredentials 要求使用 allowedOriginPatterns 而非 allowedOrigins("*")
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许的来源：开发阶段为 *，生产环境通过配置文件收敛为具体域名
                .allowedOriginPatterns(corsOriginPatterns)
                // 允许的 HTTP 方法：覆盖前后端交互全部场景
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                // 允许所有请求头（含携带 Token 的 Authorization 头）
                .allowedHeaders("*")
                // 允许携带 Cookie 等凭证信息
                .allowCredentials(true)
                // 预检结果缓存 1 小时，减少浏览器重复发送 OPTIONS 预检
                .maxAge(3600);
    }

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
                        // 说明：/product/search、/product/detail/**、/product/categories 与 /uploads/**
                        // 属于「公开但可选鉴权」，由 JwtInterceptor 的 jwt.public-paths 处理，
                        // 不能在这里排除——否则登录用户访问时登录态为空，拿不到「是否已收藏」等信息
                        // 管理后台：登录接口必须放行，否则没有 Token 就永远登不进去
                        "/admin/login",
                        // 健康检查放行：供 Docker HEALTHCHECK 与部署脚本在未登录时探测
                        "/health",
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
        // 管理后台权限拦截：/admin/** 统一校验管理员角色，/admin/login 已在上层放行
        // 放在 JWT 拦截器之后注册（先登录态解析、再角色校验）
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }
}
