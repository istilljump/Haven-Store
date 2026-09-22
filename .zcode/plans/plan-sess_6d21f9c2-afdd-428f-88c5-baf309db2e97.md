## LBS附近查询和AI智能估价功能实施计划

### 第一轮：功能实现（基础版本）

#### 1. LBS附近查询功能
**目标**：实现基于经纬度的附近商品查询功能

**实施步骤**：
1. **定义数据模型**
   - 创建 `ProductNearbyQueryDTO` - 查询参数DTO
   - 创建 `ProductNearbyVO` - 查询结果VO
   - 添加经纬度相关常量到 `ProductConstant`

2. **实现Mapper层**
   - 创建 `ProductMapper` 接口，添加 `selectNearbyProducts` 方法
   - 创建对应的XML映射文件，实现距离计算SQL

3. **实现服务层**
   - 创建 `ProductService` 接口，添加 `findNearbyProducts` 方法
   - 在 `ProductServiceImpl` 中实现业务逻辑
   - 实现矩形范围预过滤算法提高性能

4. **实现Controller层**
   - 创建 `ProductController`，添加 `findNearbyProducts` 接口
   - 添加API文档和参数校验

#### 2. AI智能估价功能
**目标**：实现基于商品特征的智能估价功能

**实施步骤**：
1. **定义数据模型**
   - 创建 `ProductEstimateDTO` - 估价参数DTO
   - 创建 `ProductEstimateVO` - 估价结果VO

2. **实现AI服务层**
   - 创建 `ProductAIService` 接口
   - 创建 `ProductAIServiceImpl` 实现类
   - 实现基础的规则引擎估价（作为占位符）

3. **集成到商品发布流程**
   - 在 `ProductServiceImpl.addProduct` 中添加AI估价建议
   - 创建 `getAIPriceSuggestion` 方法

4. **实现Controller接口**
   - 添加 `getAIPriceSuggestion` 接口
   - 添加API文档

### 第二轮：Bug修复与优化

**预期问题及修复方案**：
1. **距离计算精度问题**：优化Haversine公式实现
2. **性能问题**：优化SQL查询，添加适当的索引
3. **AI模型准确性**：实现模型训练和优化机制
4. **边界条件处理**：处理无效经纬度、异常数据等

### 第三轮：测试完善

**测试覆盖**：
1. **单元测试**
   - `ProductServiceImplTest` - 服务层逻辑测试
   - `ProductAIServiceImplTest` - AI估价逻辑测试

2. **集成测试**
   - LBS查询功能端到端测试
   - AI估价功能端到端测试

3. **边界测试**
   - 经纬度边界值测试
   - 查询半径边界测试
   - 价格预测边界测试

### 技术实现要点

**LBS附近查询**：
- 使用Haversine公式计算球面距离
- 矩形范围预过滤提高查询性能
- MyBatis-Plus分页机制
- 数据库经度索引优化

**AI智能估价**：
- 基于规则的初步实现（占位符）
- 特征提取（标题、描述、成色、分类）
- 价格预测接口设计
- 结果展示和用户建议

### 代码结构规划

```
backend/src/main/java/com/example/
├── product/
│   ├── dto/
│   │   ├── ProductNearbyQueryDTO.java  # LBS查询DTO
│   │   └── ProductEstimateDTO.java    # AI估价DTO
│   ├── vo/
│   │   ├── ProductNearbyVO.java      # LBS查询VO
│   │   └── ProductEstimateVO.java    # AI估价VO
│   ├── service/
│   │   ├── ProductService.java       # 现有服务接口
│   │   ├── ProductServiceImpl.java   # 现有服务实现
│   │   ├── ProductAIService.java     # AI估价服务接口（新增）
│   │   └── ProductAIServiceImpl.java # AI估价服务实现（新增）
│   ├── mapper/
│   │   └── ProductMapper.java        # 现有Mapper
│   └── controller/
│       └── ProductController.java    # 现有Controller
└── constant/
    └── ProductConstant.java          # 现有常量类
```

### 实施时间预估
- 第一轮功能实现：2-3天
- 第二轮Bug修复：1-2天  
- 第三轮测试完善：1-2天

### 质量保证
- 代码遵循现有编码规范
- 充分单元测试覆盖
- 文档完整（API文档、过程记录）
- Git提交规范

这个计划充分利用了现有系统的架构和最佳实践，确保新功能与现有代码无缝集成。</think>
<arg_key>allowedPrompts</arg_key>
<arg_value>[{"tool": "Bash", "prompt": "运行单元测试"}, {"tool": "Bash", "prompt": "启动应用进行回归测试"}, {"tool": "Bash", "prompt": "Git 提交代码"}, {"tool": "Bash", "prompt": "编写过程记录文档"}]