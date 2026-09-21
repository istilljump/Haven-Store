# 多阶段构建 Dockerfile
# 第一阶段：构建前端应用
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 package-lock.json
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源代码
COPY . .

# 构建生产版本
RUN npm run build

# 第二阶段：生产环境
FROM nginx:alpine

# 安装必要的工具
RUN apk add --no-cache curl

# 创建非root用户
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nextjs -u 1001

# 复制nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

# 从构建阶段复制构建结果
COPY --from=builder --chown=nextjs:nodejs /app/dist /usr/share/nginx/html

# 设置权限
RUN chown -R nextjs:nodejs /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]