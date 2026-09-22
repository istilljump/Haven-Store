package com.example.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Knife4j（Swagger2）接口文档配置类
 * <p>
 * 文档访问地址：http://localhost:8080/api/doc.html
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Configuration
public class Knife4jConfig {

    /**
     * 构建 API 文档配置对象（Docket）
     *
     * @return Docket 文档配置对象
     */
    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                // 文档基本信息
                .apiInfo(apiInfo())
                .select()
                // 只扫描标注 @ApiOperation 注解的方法，避免暴露无关接口
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                // 拦截所有路径
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 文档描述信息
     *
     * @return ApiInfo 文档信息对象
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                // 文档标题
                .title("二手商品交易平台 API")
                // 文档描述
                .description("线下融合型二手商品交易平台后端接口文档")
                // 文档版本
                .version("1.0.0")
                .build();
    }
}
