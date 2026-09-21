package com.example.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置类
 * <p>
 * 主键自增策略已通过 application.yml 的 mybatis-plus.global-config.db-config.id-type=auto
 * 与实体类 @TableId(type = IdType.AUTO) 双重保障
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链：包含 MySQL 分页插件
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：指定 MySQL 方言，自动为查询 SQL 追加 LIMIT 分页语句
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大查询条数限制为 200，防止恶意大分页拖垮数据库
        paginationInterceptor.setMaxLimit(200L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
