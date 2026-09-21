# 使用Java 17基础镜像
FROM openjdk:17-jre-slim

# 设置时区
ENV TZ=Asia/Shanghai

# 设置工作目录
WORKDIR /app

# 复制jar文件
COPY target/secondhand-market-*.jar app.jar

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]