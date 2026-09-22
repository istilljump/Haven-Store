# Docker构建和部署脚本 (PowerShell版本)
# 使用方法: .\deploy.ps1 [build|start|stop|restart|status|logs|clean]

param(
    [Parameter(Mandatory=$false)]
    [string]$Action = "help"
)

# 设置颜色输出
$RED = [ConsoleColor]::Red
$GREEN = [ConsoleColor]::Green
$YELLOW = [ConsoleColor]::Yellow
$NC = [ConsoleColor]::White

# 项目根目录
$PROJECT_DIR = $PSScriptRoot
Write-Host "${GREEN}项目目录: $PROJECT_DIR${NC}" -ForegroundColor $GREEN

# 函数：打印提示信息
function Print-Info {
    param([string]$Message)
    Write-Host "${YELLOW}[INFO] $Message${NC}" -ForegroundColor $YELLOW
}

# 函数：打印成功信息
function Print-Success {
    param([string]$Message)
    Write-Host "${GREEN}[SUCCESS] $Message${NC}" -ForegroundColor $GREEN
}

# 函数：打印错误信息
function Print-Error {
    param([string]$Message)
    Write-Host "${RED}[ERROR] $Message${NC}" -ForegroundColor $RED
}

# 检查Docker是否安装
function Test-DockerInstalled {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Print-Error "Docker未安装，请先安装Docker"
        exit 1
    }
    
    if (-not (Get-Command docker-compose -ErrorAction SilentlyContinue)) {
        Print-Error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    }
}

# 构建Docker镜像
function Build-Images {
    Print-Info "开始构建Docker镜像..."
    
    # 构建前端镜像
    Print-Info "构建前端镜像..."
    Push-Location "$PROJECT_DIR\frontend"
    if (docker build -t secondhand-frontend .) {
        Print-Success "前端镜像构建成功"
    } else {
        Print-Error "前端镜像构建失败"
        exit 1
    }
    
    # 构建后端镜像
    Print-Info "构建后端镜像..."
    Push-Location "$PROJECT_DIR\backend"
    $jarFiles = Get-ChildItem "target\secondhand-market-*.jar" -ErrorAction SilentlyContinue
    if (-not $jarFiles) {
        Print-Error "后端jar文件不存在，请先执行mvn clean package"
        exit 1
    }
    
    if (docker build -t secondhand-backend .) {
        Print-Success "后端镜像构建成功"
    } else {
        Print-Error "后端镜像构建失败"
        exit 1
    }
    
    Pop-Location
    Pop-Location
}

# 启动服务
function Start-Services {
    Print-Info "启动服务..."
    
    # 确保构建完成
    Print-Info "检查Docker镜像..."
    $frontendImage = docker images secondhand-frontend -q
    $backendImage = docker images secondhand-backend -q
    if (-not $frontendImage -or -not $backendImage) {
        Print-Info "镜像不存在，正在构建..."
        Build-Images
    }
    
    # 启动服务
    Push-Location $PROJECT_DIR
    if (docker-compose up -d) {
        Print-Success "服务启动成功"
        Print-Info "查看服务状态: .\deploy.ps1 status"
        Print-Info "查看服务日志: .\deploy.ps1 logs"
    } else {
        Print-Error "服务启动失败"
        exit 1
    }
    Pop-Location
}

# 停止服务
function Stop-Services {
    Print-Info "停止服务..."
    Push-Location $PROJECT_DIR
    if (docker-compose down) {
        Print-Success "服务停止成功"
    } else {
        Print-Error "服务停止失败"
        exit 1
    }
    Pop-Location
}

# 重启服务
function Restart-Services {
    Print-Info "重启服务..."
    Stop-Services
    Start-Services
}

# 查看服务状态
function Get-ServicesStatus {
    Print-Info "服务状态："
    Push-Location $PROJECT_DIR
    docker-compose ps
    
    Print-Info "查看实时日志: .\deploy.ps1 logs"
    Print-Info "查看前端日志: docker logs secondhand-frontend"
    Print-Info "查看后端日志: docker logs secondhand-backend"
    Print-Info "查看MySQL日志: docker logs secondhand-mysql"
    Print-Info "查看Redis日志: docker logs secondhand-redis"
    Pop-Location
}

# 查看服务日志
function Show-ServicesLogs {
    Print-Info "服务日志（按Ctrl+C退出）："
    Push-Location $PROJECT_DIR
    docker-compose logs -f
}

# 清理资源
function Clean-Services {
    Print-Info "清理Docker资源..."
    
    # 停止并删除容器
    Push-Location $PROJECT_DIR
    docker-compose down -v
    
    # 删除镜像
    Print-Info "删除Docker镜像..."
    docker rmi -f secondhand-frontend 2>$null
    docker rmi -f secondhand-backend 2>$null
    docker rmi -f $(docker images -f "dangling=true" -q) 2>$null
    
    # 清理Docker网络
    docker network prune -f
    
    Pop-Location
    Print-Success "资源清理完成"
}

# 健康检查
function Test-ServicesHealth {
    Print-Info "执行健康检查..."
    
    # 检查前端
    if (curl -f http://localhost:80 2>$null) {
        Print-Success "前端服务正常"
    } else {
        Print-Error "前端服务异常"
    }
    
    # 检查后端
    if (curl -f http://localhost:8080/api/health 2>$null) {
        Print-Success "后端服务正常"
    } else {
        Print-Error "后端服务异常"
    }
}

# 数据库迁移
function Invoke-DatabaseMigration {
    Print-Info "执行数据库迁移..."
    Push-Location "$PROJECT_DIR\backend"
    java -jar target/secondhand-market-*..jar --spring.profiles.active=prod --spring.datasource.initialize=true
    Pop-Location
}

# 备份数据库
function Backup-Database {
    Print-Info "备份数据库..."
    $backupFile = "backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
    docker exec secondhand-mysql mysqldump -u root -p123456 secondhand_market | Out-File $backupFile
    Print-Success "数据库备份完成: $backupFile"
}

# 主逻辑
switch ($Action) {
    "build" {
        Test-DockerInstalled
        Build-Images
    }
    "start" {
        Test-DockerInstalled
        Start-Services
    }
    "stop" {
        Test-DockerInstalled
        Stop-Services
    }
    "restart" {
        Test-DockerInstalled
        Restart-Services
    }
    "status" {
        Test-DockerInstalled
        Get-ServicesStatus
    }
    "logs" {
        Test-DockerInstalled
        Show-ServicesLogs
    }
    "clean" {
        Test-DockerInstalled
        Clean-Services
    }
    "health" {
        Test-DockerInstalled
        Test-ServicesHealth
    }
    "migrate" {
        Test-DockerInstalled
        Invoke-DatabaseMigration
    }
    "backup" {
        Test-DockerInstalled
        Backup-Database
    }
    default {
        Write-Host "${GREEN}二手商品交易市场 Docker 部署脚本${NC}" -ForegroundColor $GREEN
        Write-Host "使用方法: .\deploy.ps1 [command]"
        Write-Host ""
        Write-Host "命令说明："
        Write-Host "  build     - 构建Docker镜像"
        Write-Host "  start     - 启动服务"
        Write-Host "  stop      - 停止服务"
        Write-Host "  restart   - 重启服务"
        Write-Host "  status    - 查看服务状态"
        Write-Host "  logs      - 查看服务日志"
        Write-Host "  clean     - 清理Docker资源"
        Write-Host "  health    - 健康检查"
        Write-Host "  migrate   - 数据库迁移"
        Write-Host "  backup    - 数据库备份"
        Write-Host ""
        Write-Host "示例："
        Write-Host "  .\deploy.ps1 start    # 启动所有服务"
        Write-Host "  .\deploy.ps1 logs     # 查看实时日志"
        Write-Host "  .\deploy.ps1 clean     # 清理资源"
        exit 1
    }
}