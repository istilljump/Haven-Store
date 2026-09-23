package com.example.health.controller;

import com.example.common.Result;
import com.example.user.mapper.UserMapper;
import com.example.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class HealthController {

    /** 用户模块数据访问对象（用于探测数据库连通性） */
    private final UserMapper userMapper;

    /** Redis 操作工具（用于探测缓存连通性） */
    private final RedisUtil redisUtil;

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
        
        // 依赖状态实测：数据库与 Redis 任一不可用都会导致部分功能异常，
        // 健康检查直接暴露出来，省去翻日志排查
        detail.setDatabaseStatus(checkDatabase());
        detail.setCacheStatus(redisUtil.isAvailable() ? "UP" : "DOWN");
        
        return Result.success(detail);
    }

    /**
     * 探测数据库连通性
     *
     * @return UP 表示可执行查询，DOWN 表示连接异常
     */
    private String checkDatabase() {
        try {
            userMapper.selectCount(null);
            return "UP";
        } catch (Exception e) {
            log.warn("数据库健康检查失败：{}", e.getMessage());
            return "DOWN";
        }
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