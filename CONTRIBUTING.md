# 贡献指南

感谢你考虑为二手商品交易市场项目做贡献！这个文档将指导你如何参与项目开发。

## 📋 目录

- [开发环境要求](#开发环境要求)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [分支管理](#分支管理)
- [问题反馈](#问题反馈)
- [功能请求](#功能请求)
- [代码审查](#代码审查)

## 🔧 开发环境要求

### 基础环境
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7+
- Git
- Maven 3.8+

### 推荐工具
- IDE: IntelliJ IDEA (Java) / VS Code (前端)
- 数据库工具: Navicat / DBeaver
- API工具: Postman / Insomnia
- 容器化: Docker & Docker Compose

## 💻 代码规范

### Java代码规范
- 使用Google Java Style Guide
- 类名使用PascalCase（如`UserService`）
- 方法名使用camelCase（如`getUserInfo`）
- 常量使用UPPER_SNAKE_CASE（如`MAX_AGE`）
- 使用空格缩进（4个空格）

### JavaScript/TypeScript代码规范
- 使用ESLint + Prettier
- 组件名使用PascalCase（如`ProductList`）
- 变量和方法使用camelCase（如`userName`）
- 文件名使用kebab-case（如`product-list.vue`）

### 命名约定
```java
// 类
public class ProductServiceImpl {}
public interface ProductService {}

// 方法
public List<Product> getProductsByCategory(Long categoryId) {}

// 常量
public static final int MAX_PAGE_SIZE = 100;
```

```javascript
// 组件
export default {
  name: 'ProductCard',
  // ...
}

// 变量
const userId = '12345';
const productCount = 10;
```

## 📝 提交规范

### 提交格式
```
<type>(<scope>): <description>

<body>

<footer>
```

### 提交类型
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式化
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建或辅助工具变动

### 示例
```
feat(product): add smart price estimation feature

- Add AI-powered price estimation algorithm
- Support for historical price comparison
- Confidence score display

Closes #123
```

```
fix(auth): resolve login timeout issue

- Fix session timeout configuration
- Update JWT token refresh logic
- Add proper error handling

Resolves #456
```

### 分支命名规范
- `main`: 主分支（生产环境）
- `develop`: 开发分支
- `feature/xxx`: 功能分支
- `bugfix/xxx`: 修复分支
- `hotfix/xxx`: 紧急修复分支

## 🌿 分支管理

### 工作流程
1. 从`develop`创建功能分支
2. 开发并提交代码
3. 推送到远程仓库
4. 创建Pull Request
5. 代码审查
6. 合并到`develop`

### 分支示例
```bash
# 创建功能分支
git checkout -b feature/product-search-develop

# 开发完成推送到远程
git push origin feature/product-search-develop

# 创建Pull Request
# 在GitHub上创建PR，描述功能和变更

# 代码审查通过后合并
git checkout develop
git merge feature/product-search-develop
```

## 🐛 问题反馈

### 报告bug
使用GitHub Issues提交bug报告，包含以下信息：

**必填信息：**
- 操作系统（Windows/macOS/Linux）
- 浏览器版本（如Chrome 120.0.6099）
- 项目版本（如v1.0.0）
- 重复步骤
- 预期行为
- 实际行为
- 错误截图

**可选信息：**
- 环境配置
- 相关日志
- 数据库状态

### Bug报告模板
```markdown
## Bug描述
简要描述遇到的问题

## 复现步骤
1. 执行操作A
2. 点击按钮B
3. 输入数据C
4. 出现错误

## 期望结果
描述应该发生的正确行为

## 实际结果
描述实际发生的错误行为

## 环境信息
- OS: Windows 11 22H2
- Browser: Chrome 120.0.6099
- Version: v1.0.0

## 截图
![错误截图](image-url)

## 相关日志
```
[ERROR] 2024-01-15 10:30:15 Database connection failed
[DEBUG] Connection string: jdbc:mysql://localhost:3306/secondhand_market
```
```

## 💡 功能请求

### 请求新功能
使用GitHub Issues提交功能请求：

**模板：**
```markdown
## 功能描述
详细描述你希望实现的功能

## 使用场景
描述这个功能将在什么场景下使用

## 预期效果
描述功能实现后的效果

## 替代方案
如果有其他解决方案，请描述

## 优先级
- [ ] 高
- [ ] 中
- [ ] 低
```

## 👀 代码审查

### 审查要点
1. **代码质量**：是否符合编码规范
2. **功能实现**：是否正确实现需求
3. **性能影响**：是否影响系统性能
4. **安全性**：是否存在安全漏洞
5. **测试覆盖**：是否有相应的测试用例

### 审查流程
1. 在PR中添加审查者
2. 等待审查反馈
3. 根据反馈修改代码
4. 确认审查通过
5. 合并代码

### 审查标准
- [ ] 代码符合项目规范
- [ ] 功能测试通过
- [ ] 单元测试覆盖率达到80%以上
- [ ] 文档已更新
- [ ] 性能测试通过
- [ ] 安全扫描通过

## 📚 开发指南

### 后端开发
1. 遵循Spring Boot最佳实践
2. 使用依赖注入和面向接口编程
3. 添加适当的注释和文档
4. 编写单元测试
5. 使用Swagger生成API文档

### 前端开发
1. 使用Vue 3组合式API
2. 组件化开发
3. 响应式设计
4. 性能优化
5. 浏览器兼容性考虑

### 数据库设计
1. 遵循数据库范式
2. 添加适当的索引
3. 使用事务保证数据一致性
4. 定期备份数据

## 🤝 成为贡献者

### 步骤1：Fork仓库
1. 访问GitHub仓库
2. 点击"Fork"按钮

### 步骤2：克隆仓库
```bash
git clone https://github.com/your-username/secondhand-market.git
cd secondhand-market
```

### 步骤3：创建分支
```bash
git checkout -b feature/your-feature-name
```

### 步骤4：开发并提交
```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 步骤5：创建Pull Request
1. 访问GitHub仓库
2. 点击"New Pull Request"
3. 选择分支
4. 填写PR描述
5. 提交PR

## 📞 联系方式

如有问题，请通过以下方式联系：

- GitHub Issues: [项目Issues页面](../../issues)
- Email: 1593504559@qq.com
- 微信: istilljump

## 📄 许可证

本项目采用MIT许可证。详情请参阅[LICENSE](LICENSE)文件。

---

感谢你的贡献！🎉