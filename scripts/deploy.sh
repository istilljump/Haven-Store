#!/bin/bash

# Docker构建和部署脚本
# 使用方法: ./deploy.sh [build|start|stop|restart|logs|clean]

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo -e "${GREEN}项目目录: ${PROJECT_DIR}${NC}"

# 函数：打印提示信息
print_info() {
    echo -e "${YELLOW}[INFO] $1${NC}"
}

# 函数：打印成功信息
print_success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

# 函数：打印错误信息
print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

# 检查Docker是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
}

# 构建Docker镜像
build_images() {
    print_info "开始构建Docker镜像..."
    
    # 构建前端镜像
    print_info "构建前端镜像..."
    cd "$PROJECT_DIR/frontend"
    if docker build -t secondhand-frontend .; then
        print_success "前端镜像构建成功"
    else
        print_error "前端镜像构建失败"
        exit 1
    fi
    
    # 构建后端镜像
    print_info "构建后端镜像..."
    cd "$PROJECT_DIR/backend"
    # jar 由后端 Dockerfile 内部的 Maven 阶段编译产出，宿主机无需预先 mvn package
    if docker build -t secondhand-backend .; then
        print_success "后端镜像构建成功"
    else
        print_error "后端镜像构建失败"
        exit 1
    fi
}

# 启动服务
start_services() {
    print_info "启动服务..."
    
    # 确保构建完成
    print_info "检查Docker镜像..."
    if ! docker images | grep -q "secondhand-frontend" || ! docker images | grep -q "secondhand-backend"; then
        print_info "镜像不存在，正在构建..."
        build_images
    fi
    
    # 启动服务
    cd "$PROJECT_DIR"
    if docker-compose up -d; then
        print_success "服务启动成功"
        print_info "查看服务状态: ./deploy.sh status"
        print_info "查看服务日志: ./deploy.sh logs"
    else
        print_error "服务启动失败"
        exit 1
    fi
}

# 停止服务
stop_services() {
    print_info "停止服务..."
    cd "$PROJECT_DIR"
    if docker-compose down; then
        print_success "服务停止成功"
    else
        print_error "服务停止失败"
        exit 1
    fi
}

# 重启服务
restart_services() {
    print_info "重启服务..."
    stop_services
    start_services
}

# 查看服务状态
status_services() {
    print_info "服务状态："
    cd "$PROJECT_DIR"
    docker-compose ps
    
    print_info "查看实时日志: ./deploy.sh logs"
    print_info "查看前端日志: docker logs secondhand-frontend"
    print_info "查看后端日志: docker logs secondhand-backend"
    print_info "查看MySQL日志: docker logs secondhand-mysql"
    print_info "查看Redis日志: docker logs secondhand-redis"
}

# 查看服务日志
logs_services() {
    print_info "服务日志（按Ctrl+C退出）："
    cd "$PROJECT_DIR"
    docker-compose logs -f
}

# 清理资源
clean_services() {
    print_info "清理Docker资源..."
    
    # 停止并删除容器
    cd "$PROJECT_DIR"
    docker-compose down -v
    
    # 删除镜像
    print_info "删除Docker镜像..."
    docker rmi -f secondhand-frontend 2>/dev/null || true
    docker rmi -f secondhand-backend 2>/dev/null || true
    docker rmi -f $(docker images -f "dangling=true" -q) 2>/dev/null || true
    
    # 清理Docker网络
    docker network prune -f
    
    print_success "资源清理完成"
}

# 健康检查
health_check() {
    print_info "执行健康检查..."
    
    # 检查前端
    if curl -f http://localhost:80 &>/dev/null; then
        print_success "前端服务正常"
    else
        print_error "前端服务异常"
    fi
    
    # 检查后端
    if curl -f http://localhost:8080/api/health &>/dev/null; then
        print_success "后端服务正常"
    else
        print_error "后端服务异常"
    fi
}

# 数据库初始化（在 MySQL 容器内执行 backend/scripts/01-schema.sql）
migrate_database() {
    print_info "执行数据库初始化..."
    echo -e "${RED}注意：该脚本含 DROP TABLE，会清空并重建所有表！${NC}"
    cd "$PROJECT_DIR"
    if docker exec -i secondhand-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 < backend/scripts/01-schema.sql; then
        print_success "数据库初始化完成"
    else
        print_error "数据库初始化失败"
        exit 1
    fi
}

# 备份数据库
backup_database() {
    print_info "备份数据库..."
    docker exec secondhand-mysql mysqldump -u root -p123456 secondhand_market > backup_$(date +%Y%m%d_%H%M%S).sql
    print_success "数据库备份完成: backup_$(date +%Y%m%d_%H%M%S).sql"
}

# 主逻辑
case "${1:-help}" in
    build)
        check_docker
        build_images
        ;;
    start)
        check_docker
        start_services
        ;;
    stop)
        check_docker
        stop_services
        ;;
    restart)
        check_docker
        restart_services
        ;;
    status)
        check_docker
        status_services
        ;;
    logs)
        check_docker
        logs_services
        ;;
    clean)
        check_docker
        clean_services
        ;;
    health)
        check_docker
        health_check
        ;;
    migrate)
        check_docker
        migrate_database
        ;;
    backup)
        check_docker
        backup_database
        ;;
    *)
        echo -e "${GREEN}二手商品交易市场 Docker 部署脚本${NC}"
        echo "使用方法: ./deploy.sh [command]"
        echo ""
        echo "命令说明："
        echo "  build     - 构建Docker镜像"
        echo "  start     - 启动服务"
        echo "  stop      - 停止服务"
        echo "  restart   - 重启服务"
        echo "  status    - 查看服务状态"
        echo "  logs      - 查看服务日志"
        echo "  clean     - 清理Docker资源"
        echo "  health    - 健康检查"
        echo "  migrate   - 数据库迁移"
        echo "  backup    - 数据库备份"
        echo ""
        echo "示例："
        echo "  ./deploy.sh start    # 启动所有服务"
        echo "  ./deploy.sh logs     # 查看实时日志"
        echo "  ./deploy.sh clean     # 清理资源"
        exit 1
        ;;
esac