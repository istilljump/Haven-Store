package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 二手商品交易平台启动类
 * <p>
 * 说明：各业务模块的 Mapper 接口均标注 @Mapper 注解，
 * 由 MyBatis-Plus 自动配置类扫描主包（com.example）下所有子包，无需额外配置 @MapperScan，
 * 后续新增业务模块时零配置扩展。
 * {@code @EnableScheduling} 启用定时任务（超时订单自动取消、商品自动下架）。
 *
 * @author ZCode
 * @date 2026/09/21
 */
@EnableScheduling
@SpringBootApplication
public class SecondHandMarketApplication {

    /**
     * 应用程序入口方法
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SecondHandMarketApplication.class, args);
    }
}
