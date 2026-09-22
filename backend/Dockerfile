# ============================================================
# 后端镜像：多阶段构建
#   阶段一用官方 Maven 镜像编译打包，阶段二只保留 JRE + jar，镜像更小且不夹带构建工具
# ============================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先只复制 pom.xml 并预热依赖，利用 Docker 层缓存加速后续构建
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码并打包（跳过测试，测试需要 MySQL/Redis）
COPY src/ src/
RUN mvn -B -q clean package -DskipTests

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre-jammy

# 设置时区
ENV TZ=Asia/Shanghai

# curl 用于容器健康检查（jammy 基础镜像不带 curl，需自行安装）
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制构建产物 jar（artifactId=second-hand-market，产物名为 second-hand-market-<version>.jar）
# 用 *.jar 通配，避免版本号变化后 COPY 找不到文件；.jar.original 后缀不同不会被匹配
COPY --from=builder /build/target/*.jar app.jar

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 暴露端口
EXPOSE 8080

# 健康检查（对应 HealthController：GET /api/health）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
