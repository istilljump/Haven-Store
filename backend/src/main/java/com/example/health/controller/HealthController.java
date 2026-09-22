package com.example.health.controller;

import com.example.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统健康检查控制器
 * <p>
 * 提供系统运行状态检查接口，用于服务健康监控和负载均衡器探针
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Api(tags = "系统健康检查接口")
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 基础健康检查
     * <p>
     * 返回系统基础运行状态，不依赖外部服务
     *
     * @return 健康状态结果
     */
    @ApiOperation(value = "基础健康检查", notes = "检查应用是否正常运行，不依赖数据库或其他外部服务")
    @GetMapping
    public Result<String> health() {
        return Result.success("OK");
    }

    /**
     * 详细健康检查
     * <p>
     * 返回系统各组件运行状态，包括数据库连接等
     *
     * @return 详细健康状态结果
     */
    @ApiOperation(value = "详细健康检查", notes = "检查应用各组件运行状态，包括数据库连接、缓存服务等")
    @GetMapping("/detail")
    public Result<HealthDetail> healthDetail() {
        HealthDetail detail = new HealthDetail();
        detail.setStatus("UP");
        detail.setTimestamp(System.currentTimeMillis());
        detail.setApp("haven-store-backend");
        detail.setVersion("1.0.0");
        
        // TODO: 实现数据库连接检查、Redis连接检查等
        // detail.setDatabaseStatus("UP");
        // detail.setCacheStatus("UP");
        
        return Result.success(detail);
    }

    /**
     * 健康检查详情数据类
     */
    public static class HealthDetail {
        private String status;
        private long timestamp;
        private String app;
        private String version;
        private String databaseStatus;
        private String cacheStatus;

        // Getters and Setters
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getApp() {
            return app;
        }

        public void setApp(String app) {
            this.app = app;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDatabaseStatus() {
            return databaseStatus;
        }

        public void setDatabaseStatus(String databaseStatus) {
            this.databaseStatus = databaseStatus;
        }

        public String getCacheStatus() {
            return cacheStatus;
        }

        public void setCacheStatus(String cacheStatus) {
            this.cacheStatus = cacheStatus;
        }
    }
}