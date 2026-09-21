# 前端功能测试方案

## 当前状况
- ✅ 前端服务正常运行 (http://localhost:3000)
- ❌ 后端服务因数据库和依赖问题无法启动
- 🎯 目标：测试前端功能完整性

## 测试方案

### 1. 模拟API测试
前端项目已经集成了API请求工具，我们可以：

**方法一：修改API指向**
```javascript
// 在 utils/request.js 中修改
const BASE_URL = process.env.NODE_ENV === 'development' 
  ? '/api'  // 开发环境使用代理
  : 'http://localhost:8080/api';
```

**方法二：使用代理**
```javascript
// vite.config.js
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  }
});
```

### 2. 测试用例设计

#### 登录功能测试
```javascript
// 模拟登录数据
const testUser = {
  username: 'admin',
  password: '123456'
};
```

#### 注册功能测试
```javascript
// 测试注册数据
const testRegistration = {
  username: 'testuser_' + Date.now(),
  password: '123456',
  confirmPassword: '123456',
  phone: '13800138000',
  nickname: '测试用户' + Date.now()
};
```

#### 商品发布测试
```javascript
// 测试商品数据
const testProduct = {
  title: '测试商品 ' + Date.now(),
  categoryId: 1,
  description: '这是测试商品描述',
  price: 999.99,
  productCondition: '九成新',
  tradeType: 'offline',
  address: '测试地址',
  longitude: 116.316833,
  latitude: 39.981013
};
```

### 3. 前端验证要点

#### 表单验证
- [ ] 用户名长度和格式验证
- [ ] 密码强度验证
- [ ] 手机号格式验证
- [ ] 商品价格数字验证
- [ ] 成色选项验证
- [ ] 交易方式联动效果

#### 交互体验
- [ ] 表单提交loading状态
- [ ] 错误提示信息
- [ ] 成功操作反馈
- [ ] 页面跳转逻辑

#### UI/UX
- [ ] 响应式布局适配
- [ ] 颜色搭配和样式统一
- [ ] 加载状态动画
- [ ] 表单布局合理性

### 4. 自动化测试脚本
```javascript
// 创建测试工具
class FrontendTester {
  testFormValidation() {
    // 测试表单验证
  }
  
  testApiRequests() {
    // 测试API请求
  }
  
  testUserFlow() {
    // 测试用户流程
  }
}
```

## 修复建议

### 1. 修复后端依赖问题
```bash
# 添加缺失的Maven依赖
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>2.2.15</version>
</dependency>
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>
```

### 2. 数据库初始化
```bash
# 先创建数据库，再启动应用
mysql -u root -p -e "CREATE DATABASE second_hand_market CHARACTER SET utf8mb4;"
```

### 3. 使用H2内存数据库进行测试
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
```

## 下一步计划
1. ✅ 完成前端功能测试
2. 🔧 修复后端编译错误
3. 🗄️ 初始化数据库
4. 🔗 完成前后端联调
5. 📋 进行代码规范优化
6. 👥 添加管理员后台
7. 🐳 创建Docker部署脚本