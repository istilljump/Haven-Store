# 闲置港湾 Haven-Store

[![CI](https://github.com/istilljump/Haven-Store/actions/workflows/ci.yml/badge.svg)](https://github.com/istilljump/Haven-Store/actions/workflows/ci.yml)

> **毕业不甩卖，闲置有归宿。** 面向校园与同城的闲置循环平台——用平台真实成交行情帮卖家定对价，用校区与安全面交点让线下交易更省心。

二手交易最难的不是"上架一个商品"，而是三件事：**卖家的闲置不知道值多少钱、同城面交约在哪里不放心、买家不知道眼前的价格亏不亏**。Haven-Store 把平台自己的成交记录变成定价行情——发布时给出可解释的建议价（"近 90 天同类成交 5 件，¥2,900~¥4,200，中位价 ¥3,600"），挂久了没人问就主动提示降价；把"附近"做成校区筛选与推荐面交点；再用已验证购买评价和举报闭环兜住信任。每一笔成交又回流为行情数据——**交易越多，估价越准的数据飞轮**。

技术栈：Spring Boot + MyBatis-Plus + MySQL 8 + Redis 7 · Vue 3 + Element Plus + ECharts · 全链路「发布 → 估价 → 校区浏览 → 私信咨询 → 下单锁货 → 面单交收 → 评价 → 举报处理」闭环，零配置 10 分钟跑通、由 422 项自动化冒烟断言验收。

## 🌟 招牌能力（2026-11 新增）

### ① 数据驱动定价（AI 定价助手 2.0）

- **成交行情估价**：以平台自身 `order_item` 成交快照为样本，按「分类 × 成色」聚合近 90 天成交，取**中位价**为建议价，依据（样本量/区间/中位价）逐条透出——不接外部 AI，纯统计口径，可解释、可断言、可复现
- **诚实降级**：同类成交样本不足（后台可设阈值，默认 3 件）时明示"数据积累中，按规则基线估价"，不假装智能
- **买家侧划算指数**：详情页显示「低于近期 X% 的同类成交价」（价格分位）
- **卖家侧降价建议**：滞销商品（挂牌超 N 天且价格高于行情九折）在「我的发布」提示"建议降至 ¥X"并可一键采纳（`POST /product/{id}/price`，定价权始终在卖家手里）
- **确定性契约**：估价不再含随机波动，同输入必同输出
- **全部可开关**：后台「交易设置」提供 行情估价 / 划算指数 / 降价建议 / 滞销天数 / 最小样本量 五项配置，缺省开启

### ② 校园 LBS 场景化

- **校区维度**：商品可挂到校区（`campus` 表，含中心坐标），列表页校区筛选，附近模式可取校区中心为圆心
- **安全面交点**：种子内置快递驿站/图书馆前/校门口等推荐面交位置，详情页按 Haversine 球面距离推荐"距商品最近的面交点"，同城面交有明确赴约地点
- **校园演示数据**：开箱即用的历史成交种子让行情、划算指数、降价建议三个能力装完就能演示

### ③ 配套增强层（搜索 / 推荐 / 趋势 / 信用）

> 叙事定位：**估价和 LBS 是核心招牌，增强搜索 + 场景推荐是围绕校园闲置场景的配套能力**——搜索把流量导向行情，推荐把行情变成转化（方案见 `docs/012`，五轮迭代评估见 `docs/013`）。

- **增强搜索**：词库/拼音/本校热搜三级实时补全（候选带【教材】【本校近期成交12件】标签）、本地词表纠错（宁可不纠不可错纠）、结果条目内嵌品类中位价与划算指数、搜索埋点 + 「点击 → 成交」归因；原 `/product/search` 接口原样保留
- **场景化推荐**：五条校园规则（R1 校区成交活跃 / R2 本校高频品类 / R3 搜索后互补闲置 / R4 详情页同校同款 / R5 详情页互补），**每条推荐强制携带可解释文案**（"推荐原因：同校区·九成新·同类闲置"），字典未命中宁可不推荐不凑数
- **行情趋势**：详情页行情卡内嵌 ECharts 迷你走势图（按日聚合，`GET /market/trend`）
- **卖家信用画像**：确定性公式（基线60 + 成交×3 +（评分-3）×10 − 有效举报×5），详情页信用徽章
- **数据飞轮**：每一笔成交自动回流——行情样本 +1、估价更准、看板/趋势/推荐排序即时反映；`python scripts/demo_flywheel.py` 一键现场演示
- **可开关**：场景化推荐 / 成色二次筛选接入后台「交易设置」；推荐与补全走 5 分钟短缓存（交易与行情随时在变，缓存只为削峰）

## 🔄 复现性声明与 10 分钟极速复现指南

### 复现性声明

本项目承诺：**在满足下述环境要求的前提下，任何人从仓库零起步，都能在 10 分钟内将系统完整跑起来，并通过 422 项自动化冒烟断言验证功能与预期一致。** 为此我们做了四件事：

1. **环境即代码**：数据库结构（18 张表：业务 14 张 + 校区/面交点 + 搜索词库/纠错/埋点/互补字典）与种子数据（含历史成交行情样本）全部由 `backend/sql/01-schema.sql` 一个脚本确定，不依赖任何手工建库步骤；后端/前端依赖版本分别由 `pom.xml` 与 `package-lock.json` 锁定。
2. **配置有默认值、可被环境变量覆盖**：所有敏感或环境相关配置（数据库口令、JWT 密钥等）都写成 `${VAR:默认值}` 形式——默认值保证零配置即可在本机跑通，部署时用环境变量覆盖即可，无需改动任何代码或配置文件。
3. **行为有断言**：`tests/api_smoke_test.py` 覆盖 422 项断言（注册、发布、行情估价与趋势、校区筛选、面交点推荐、增强搜索补全/纠错/埋点、场景化推荐三位置、卖家信用、降价建议、下单支付、私信、举报闭环、管理端全部页面、导出、设置生效等），`mvn test` 另有 99 项单元测试（含行情聚合与信用公式的精确值断言）。复现是否成功不看"页面能打开"的感觉，看这两组测试是否全绿。
4. **无外部服务依赖**：不依赖任何真实支付网关、短信/邮件服务或付费 AI 接口（估值为平台成交数据的统计模型 + 规则基线降级），MySQL 8 与 Redis 7 是仅有的两个外部依赖，且仓库附带免管理员一键拉起脚本。

**已知非确定性因素**（不影响功能复现，仅影响具体数值）：所有时间戳取运行时刻（行情种子按 `NOW()` 相对天数写入，统计窗口因此始终有效）；并发场景下商品被谁买走取决于竞争顺序。估价本身**不含随机波动**——同数据必得同结果。除此之外，相同输入必然得到相同输出。

### ⏱ 10 分钟极速复现（Windows 一键路线）

> 全程不需要管理员权限。每一步都标注了预期耗时，超时即说明环境有缺项，请对照下文依赖清单排查。

| 步骤 | 操作 | 耗时 | 预期结果 |
|---|---|---|---|
| 1 | 克隆仓库 `git clone https://github.com/istilljump/Haven-Store.git && cd Haven-Store` | 1 min | 拿到全部源码 |
| 2 | 双击 `scripts\start-dev-services.bat` | 1-2 min | 弹出 MySQL、Redis 两个窗口；首次运行自动建库 `secondhand_market` 并导入种子数据 |
| 3 | 双击 `scripts\rebuild-backend.bat`（或 `cd backend && mvn clean package -DskipTests`） | 2-4 min | 生成 `backend/target/second-hand-market-1.0.0.jar` |
| 4 | 双击 jar 或 `java -jar backend\target\second-hand-market-1.0.0.jar` | 15 s | 控制台出现 `Started SecondHandMarketApplication` |
| 5 | `cd frontend && npm install`（首次） | 2-3 min | 依赖安装完成 |
| 6 | `cd frontend && npm run dev` | 10 s | Vite 启动，浏览器打开 http://localhost:3000 |
| 7 | 验证：`python tests/api_smoke_test.py` | 1 min | 输出 `通过: 422   失败: 0` |

**验收账号**（由种子数据写入）：管理员 `admin / admin123`（后台 http://localhost:3000/admin），普通用户 `testuser1 / 123456`、`testuser2 / 123456`。种子数据自带 2 件在售商品（二手 iPhone 12、编程书籍套装）与 8 个分类。

**Linux/Mac 路线**：`docker compose -f config/docker-compose.yml up -d`（`config/docker-compose.yml` 会自动建库导种子），随后后端 `mvn clean package -DskipTests && java -jar backend/target/*.jar`、前端 `npm install && npm run dev`，其余相同。

### 📦 依赖清单

| 组件 | 版本要求 | 用途 | 备注 |
|---|---|---|---|
| JDK | 8+（推荐 17/22，Docker 镜像基于 17） | 后端编译运行 | `mvn` 需在 PATH |
| Maven | 3.6+ | 后端构建 | 仓库 `.tools/` 内自带 3.9.16 可用 |
| Node.js | 18+（含 npm） | 前端构建与开发 | 前端版本锁定见 `frontend/package-lock.json` |
| MySQL | 8.0+ | 业务数据库 | 建库脚本见 `backend/sql/` |
| Redis | 7+（6.x 可用） | 登录缓存/登录锁定/设置 | |
| Python | 3.8+（仅测试用） | 运行冒烟测试 | 无第三方依赖，纯标准库 |

后端关键依赖（完整清单见 `backend/pom.xml`）：Spring Boot **2.7.12**、MyBatis-Plus **3.5.3.1**、JJWT **0.11.5**、Knife4j **4.1.0**、mysql-connector-j **8.0.33**。前端关键依赖（完整清单见 `frontend/package.json`）：Vue **^3.4**、Element Plus **^2.4**、ECharts **^5.5**、Axios **^1.6**。

### 🔑 环境变量模板

将以下内容保存为启动前可用的环境变量（**所有项都有默认值，本机复现可以一个都不设**；部署到公网前必须覆盖带 ⚠️ 的项）：

```bash
# ===== 数据库 =====
DB_HOST=localhost                    # MySQL 主机
DB_PORT=3306                         # MySQL 端口
DB_NAME=secondhand_market            # 库名（与建表脚本一致）
DB_USERNAME=root                     # 数据库账号
DB_PASSWORD=1234                     # ⚠️ 部署必改

# ===== Redis =====
REDIS_HOST=localhost
REDIS_PORT=6379

# ===== JWT =====
JWT_SECRET=change-me-to-a-random-secret-at-least-32-bytes   # ⚠️ 部署必改（HS256 要求 ≥32 字节）
JWT_EXPIRATION=604800000             # Token 默认有效期（毫秒，7 天）；后台「安全设置」可覆盖

# ===== 上传（可选，默认 ./uploads）=====
FILE_UPLOAD_DIR=./uploads
```

设置方式（任选其一）：

```bash
# Linux / Mac
export DB_PASSWORD='your-strong-password'
export JWT_SECRET='a-very-long-random-secret-at-least-32-bytes'

# Windows PowerShell
$env:DB_PASSWORD='your-strong-password'
$env:JWT_SECRET='a-very-long-random-secret-at-least-32-bytes'

# Docker Compose：写入 backend.environment（见 config/docker-compose.yml）
```

### ✅ 复现成功的判定标准

1. `curl http://localhost:8080/api/health` 返回 `{"code":200,...}`；
2. 用 `testuser1 / 123456` 能登录前端并在商品列表看到「二手iPhone 12」；
3. `python tests/api_smoke_test.py` 输出 **通过: 422 失败: 0**；
4. `cd backend && mvn test` 输出 **Tests run: 73, Failures: 0, Errors: 0**。

## 🆕 本轮升级一览（2026-11 · 第二轮五连迭代）

> 引导式迭代：每轮先评估裁定、再实施回归；全程记录见 `docs/012`（轮2 方案）与 `docs/013`（轮3~5 评估裁定）。

- **轮2 增强搜索**：`com.example.search` 模块（词库/拼音/本校热搜三级补全、本地词表纠错、结果内嵌行情标签、搜索埋点 + 成交归因）；4 张新表（全库 18 张）
- **轮2 场景化推荐**：`com.example.recommend` 模块，五条校园规则 + 强制可解释文案 + 互补品类字典；总开关接入后台
- **轮2 数据飞轮**：首页「本校行情速览」看板 + `scripts/demo_flywheel.py` 一键演示（成交一笔 → 样本 +1 → 看板/估价/推荐即时反映）
- **轮3 行情趋势**：`/market/trend` 按日聚合 + 详情页 ECharts 迷你走势图（惰性加载）
- **轮4 卖家信用**：确定性公式信用分（成交/评分/举报三维）+ `/credit/{userId}` + 详情页信用徽章
- **轮5 性能收口**：补全/推荐接 5 分钟 Redis 短缓存（null/空不缓存，开关关闭永不落缓存）
- **测试**：冒烟 370 → **422**（章节 33~37）、单测 73 → **99**；文档叙事归位（招牌 vs 配套增强层）

## 🆕 上一轮升级一览（2026-11 第一轮）

> 本轮把项目从「通用二手平台」重塑为「校园闲置循环平台 × 数据驱动定价」，方案全文见 `docs/011-项目定位重塑方案.md`。

- **品牌叙事**：中文副品牌「闲置港湾 Haven-Store」+ 口号"毕业不甩卖，闲置有归宿"，落地到首页/登录页/页面标题/站点设置
- **新模块 `market`（成交行情）**：`GET /market/summary` 查询「分类×成色」近 90 天成交基线（样本量/区间/中位价，游客可用）；估价双路径 = 成交中位价（MARKET_DATA）→ 样本不足降级规则基线（RULE_BASELINE）；**移除 ±200 随机抖动**
- **新模块 `campus`（校园场景）**：`GET /campus/list`、`GET /campus/{id}/meet-points`、`GET /campus/nearest-meet-point`（Haversine 距离）；`product.campus_id` + 列表校区筛选 + 发布向导选校区 + 详情页推荐面交点
- **新接口**：`POST /product/{id}/price`（卖家改价，仅卖家/仅在售）；`/product/search` 新增 `campusId` 参数
- **详情页新字段**：`marketBasis`（行情基线）、`pricePosition`（价格分位）、`campusName`；「我的发布」新字段 `priceSuggestion`（降价建议）
- **设置扩展**：`trade.marketBasis / dealBadge / priceSuggest / staleDays / minSampleSize` 五项后台开关（缺省开启，老配置缺字段按缺省处理）；公开配置接口同步透出
- **数据库**：14 张表（+`campus`、`meet_point`），`product` 加 `campus_id`；种子新增历史成交 15 件（四个「分类×成色」组合的中位价开箱可演示）与滞销演示商品 1 件；老库执行 `backend/sql/03-upgrade.sql` 幂等升级
- **测试**：冒烟 340 → **370**（新增行情估价确定性、降级路径、校区筛选、面交点距离、降价建议与改价权限等断言）；单测 66 → **73**（新增 `MarketDataServiceImplTest`，估价改精确值断言）
- **前端**：发布向导估价依据展示、详情页行情卡+划算指数+面交点、列表校区筛选、我的发布一键降价、品牌文案与站点名

## 🆕 上一轮升级一览（2026-10）

### 安全与正确性（必修项）
- **修复系统消息伪造漏洞**：移除任何登录用户可调用的 `POST /message/send`，群发能力只保留在管理后台
- **禁用账号立即失效**：JWT 拦截器每个请求校验账号状态（Redis 缓存优先），禁用后无法继续下单/发帖/私信
- **登录防爆破**：按后台「安全设置」失败 N 次锁定 M 分钟（Redis 计数）
- **密码强度策略生效**：注册/改密按后台设置校验（low/medium/high）
- **上传魔数校验**：文件头与扩展名不符即拒绝（防伪装脚本）；大小/类型白名单由后台「上传设置」驱动
- **Redis 序列化加固**：反序列化类型白名单收敛 + 缓存不再存 BCrypt 密文
- **支付/确认收货条件更新**：并发下不会重复推进订单状态
- **CSV 公式注入防护**；修复分类排序假保存、设置页电话校验挡保存、Logo 假上传等假功能

### 普通用户端新增
- **真首页**：搜索框 + 分类导航 + 最新商品流 + 附近好物入口
- **通知中心**（/notifications）：系统公告收件箱 + 顶栏铃铛角标
- **订单详情页**（/orders/:orderNo）：买家与订单内商品卖家均可查看
- **我的发布**（/my/products）：独立管理页，支持下架/重新上架/彻底删除/分页筛选
- **AI 智能估价入口**：发布向导第 1 步一键回填建议价（后台可关闭）
- **附近商品模式**：商品列表页一键定位按距离浏览
- **评价与订单挂钩**：仅购买过（已支付/已完成）的用户可评价，评论带「已验证购买」标识
- **商品举报**：详情页一键举报，与后台处理闭环
- **搜索增强**：关键词覆盖标题/描述/品牌/型号 + 排序 UI（最新/价格升降）
- **订单超时自动取消**：30 分钟未支付自动取消并释放商品（定时任务）
- **私信体验**：发送乐观插入上屏、移动端上下堆叠布局

### 管理员端新增
- **订单管理**（/admin/orders）：全平台订单查询/详情/CSV 导出
- **评论管理**（/admin/comments）：隐藏/恢复/删除评论
- **举报处理台**（/admin/reports）：驳回举报 / 下架商品 / 隐藏评论，处理留痕
- **用户管理增强**：编辑资料（昵称/手机号/角色）、重置密码
- **数据看板**：近 7 日新增趋势折线图 + 商品分类分布饼图（ECharts）
- **系统设置落库并真实生效**：新表 `system_setting`；上传限制、价格上限、AI 估价开关、登录锁定、会话超时（JWT 时效）、商品自动下架（autoOfflineHours）全部接入业务；公开配置接口 `GET /api/public/settings` 供前端读取站点信息

### 体验优化
- 全站共享组件：`AppImage`（坏链兜底+懒加载）、`ProductCard`（统一商品卡片）
- 顶栏全局请求进度条、favicon、页面标题、移动端汉堡菜单（前台+后台）
- 清理死代码与遗留假接口（孤儿发布页、fetch 版登录、后端不存在的 /user/** 封装等）
- ESLint/Prettier 配置补齐；`@element-plus/icons-vue`、`echarts` 显式声明依赖
- 分类列表 Redis 缓存（管理端变更自动驱逐）；评分改 SQL AVG

### 数据库升级
- 全新安装直接执行 `backend/sql/01-schema.sql`（已含全部 18 张表与行情/校园/搜索词库种子）
- **2026-10 老库升级执行一次 `backend/sql/02-upgrade.sql`**：`system_message` 加群发批次号 `batch_no`，新增 `report`（举报）与 `system_setting`（系统设置）两张表
- **2026-11 老库升级执行一次 `backend/sql/03-upgrade.sql`**：新增 `campus`（校区）与 `meet_point`（面交点）表、`product.campus_id` 列、历史成交行情种子与滞销演示商品，并幂等同步品牌文案；可重复执行
- **2026-11 第二轮老库升级执行一次 `backend/sql/04-upgrade.sql`**：新增 `search_suggest_word`（补全词库）/`search_correction`（纠错词表）/`search_log`（搜索埋点）/`category_relation`（互补品类字典）四张表与全部字典种子；可重复执行

### 测试
- 冒烟测试 `tests/api_smoke_test.py` 扩至 336 项断言，覆盖注册、举报闭环、管理端订单/评论、看板趋势、设置生效等新能力
- 修复从未通过过的 66 项单元测试（对齐服务契约与估价随机波动），`mvn test` 全绿

### 回归修复（2026-10-16）
- **卖家可在商品会话里回话**：私信会话键含商品 ID，卖家回复必然带 `productId`，此前被「不能咨询自己发布的商品」一律拒绝。现按商品归属分治——买家只能咨询商品发布者；卖家仅在「该商品会话已存在」时可回复，从自己商品详情页凭空开会话仍被拒；同时新增 3 项冒烟断言（336 → 340）
- **管理后台顶栏不再显示上一个账号的昵称**：`/admin/login` 与 `/admin/**` 共用同一个 `AdminLayout` 实例，登录后布局不重新挂载，而顶栏读的是 localStorage 快照。现统一改为读取 Pinia store 的响应式身份数据，管理端登录与退出均走 store（退出同时清零未读角标）
- **商品详情页「发布时间」显示绝对时间**：该字段原用相对时间渲染，新发布的商品永远显示「刚刚」，看不出具体上架日期；同时修复相对时间格式化把「解析失败」与「未来时间」都当成「刚刚」的兜底逻辑

## 🚀 快速开始

### 环境要求

- JDK 8+（推荐 17；Docker 镜像基于 JDK 17）
- Node.js 18+
- MySQL 8.0+
- Redis 7+
- Docker & Docker Compose (可选)

### 方式一：Docker部署（推荐）

1. **克隆项目**
```bash
git clone https://github.com/istilljump/Haven-Store.git
cd Haven-Store
```

2. **构建并启动服务**
```bash
# Linux/Mac
chmod +x scripts/deploy.sh
./scripts/deploy.sh start

# Windows
.\scripts\deploy.ps1 start
```

3. **访问应用**
- 前端：http://localhost
- 后端API：http://localhost:8080
- 管理后台：http://localhost/admin

4. **默认账号**（由 `backend/sql/01-schema.sql` 初始化）

| 用户名 | 密码 | 说明 |
|---|---|---|
| `admin` | `admin123` | 管理员 |
| `testuser1` | `123456` | 普通用户（含 2 条示例商品） |
| `testuser2` | `123456` | 普通用户 |

### 方式二：本地部署

#### Windows 一键启动（推荐给不熟悉命令行的同学）

`scripts/` 目录下提供了四个双击即可运行的脚本（已按 GBK 编码，中文显示正常）：

| 脚本 | 用途 |
|---|---|
| `scripts/start-project.bat` | 一键启动：检查 MySQL/Redis（没运行会自动拉起）→ 起后端 → 起前端。之后浏览器打开 http://localhost:3000 |
| `scripts/start-dev-services.bat` | 单独启动本机 MySQL 与 Redis（start-project.bat 会自动调用，也可单独双击） |
| `scripts/stop-dev-services.bat` | 一键停止：结束后端/前端进程，安全关闭 Redis 与 MySQL（数据落盘）；与 start-dev-services.bat 配对使用 |
| `scripts/rebuild-backend.bat` | 改过 Java 代码后重新编译打包（本机专用路径） |
| `scripts/init-database.bat` | 初始化/重置数据库（会清空数据，慎用） |

> 关掉项目：把弹出的所有黑窗口（后端、前端、MySQL、Redis）都关掉即可。
> 窗口在运行期间**不要关闭**；MySQL / Redis 的窗口关掉就等于停掉对应服务。

> **关于 MySQL 的启动方式**：本机 MySQL 是以 Windows 服务（服务名 `MySQL80`）方式安装的，
> 且启动类型是「手动」，不会开机自启。若你的账号有管理员权限，用管理员身份执行
> `net start MySQL80` 即可；若没有（`net start` 报「拒绝访问」），
> 双击 `scripts/start-dev-services.bat`——它会在**不需要管理员权限**的前提下，
> 用已安装的 `mysqld.exe` 加一个独立数据目录（`.tools\mysql-data`，已加入 .gitignore）
> 把 MySQL 跑起来，首次运行会自动建库并导入种子数据。

#### 前置：初始化数据库

先启动 MySQL，然后执行建表脚本：

```bash
mysql -uroot -p < backend/sql/01-schema.sql
```

> Windows 若提示 `'mysql' 不是内部或外部命令`，说明 MySQL 的 bin 目录没加入 PATH，
> 改用完整路径即可（或直接双击 `scripts/init-database.bat`）：
> `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p < backend\sql\01-schema.sql`
>
> 脚本会创建数据库 `secondhand_market`（10 张表 + 种子数据），并写入默认账号：
> `admin/admin123`（管理员）、`testuser1/123456`、`testuser2/123456`。
> 库名必须与 `application.yml` 中的 JDBC URL 一致。

#### 1. 后端部署

```bash
cd backend
mvn clean package -DskipTests
java -jar target/second-hand-market-*.jar
```
> 产物名以 `pom.xml` 的 `artifactId` 为准（当前为 `second-hand-market-1.0.0.jar`，注意带连字符）。
> 单元测试需要本机已启动 MySQL 与 Redis 并完成建表，否则请加 `-DskipTests` 跳过测试。
> 后端默认连接 `localhost:3306/secondhand_market` 与 `localhost:6379`，
> 账号密码在 `backend/src/main/resources/application.yml` 中按本地环境修改。
> 启动后接口地址带 `/api` 前缀，例如 `http://localhost:8080/api/user/login`。

#### 2. 前端部署

```bash
cd frontend
npm install
npm run dev      # 开发模式，访问 http://localhost:3000
npm run build    # 生产构建，产物在 dist/
```

### 方式三：开发模式

#### 后端开发

```bash
cd backend
mvn spring-boot:run
```

#### 前端开发

```bash
cd frontend
npm run dev
```

## 🔧 管理功能

### 管理员后台

访问 `/admin` 进入管理后台，支持以下功能：

- **数据概览**：实时查看平台数据统计
- **用户管理**：用户列表、状态管理、权限控制
- **商品管理**：商品审核、下架、删除
- **分类管理**：分类管理、排序设置
- **系统消息**：发送系统公告
- **系统设置**：站点配置、交易规则、安全设置

### API管理

```bash
# 查看API文档（Knife4j，基于 Swagger2）
http://localhost:8080/api/doc.html

# 健康检查
curl http://localhost:8080/api/health
```

## 🛒 购物车与订单

### 通行的设计：收藏与购物车是同一张表

两者都落在 `user_product_relation` 表（`relation_type` 分别是 `collect` 与 `cart`），
所以「购物车 ↔ 收藏」的互转就是删一条关系再加一条，不需要两套表两套接口。
购物车页因此做成双 Tab：**购物车** 与 **我的收藏**，任意一边都能一键移到另一边，
「全部加入购物车」也会分别告诉你成功几件、因已售出/已下架跳过几件。

二手商品都是单件，购物车**不做数量加减**，一件商品就是一条记录
（`(user_id, product_id, relation_type)` 唯一索引天然防重，重复加购不报错）。

### 下单与支付

订单生命周期：**待支付 → 已支付 → 已完成**，待支付可取消。

- **下单即锁定商品**：创建订单时把商品置为「已售出」，并采用
  `UPDATE ... WHERE status = 1` 的条件更新——更新影响行数为 0 说明刚被别人买走，
  整个下单事务回滚并明确提示。这样同一件二手商品不会被两个人买走。
- **取消即释放**：取消待支付订单时把商品恢复为「在售」（只恢复仍处于「已售出」的，
  不会覆盖卖家的下架操作）。
- **订单明细存快照**：`order_item` 保存下单时的标题、封面与成交价，
  商品之后被改价、改名、甚至删除，历史订单展示的仍是当时的成交信息。
- ⚠️ **支付是模拟支付**：项目**未接入任何真实支付渠道**，点「去支付」只推进订单状态、
  写入支付时间，**不产生任何真实扣款**。界面与接口文档都做了明确标注，
  接入真实支付需要另外对接支付网关。
- 买卖双方都能看到订单：买家在「我的订单 → 我买到的」，卖家切到「我卖出的」
  可以看到包含自己商品的订单与买家昵称。

主要接口（全部需要登录，归属取自登录态）：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/cart/{productId}` | 加入购物车（幂等，返回 false 表示已在购物车中） |
| DELETE | `/cart/{productId}` | 移出购物车 |
| GET | `/cart/list` | 购物车列表（含是否可结算与不可结算原因） |
| GET | `/cart/count` | 购物车件数（头部角标） |
| POST | `/cart/{productId}/move-to-favorite` | 移入收藏 |
| POST | `/cart/favorites/add-all` | 收藏批量加入购物车 |
| POST | `/order/create` | 用勾选的商品下单（支持买家留言） |
| GET | `/order/list?status=` | 我的订单（买家视角，可按状态筛选） |
| GET | `/order/sold` | 我卖出的（卖家视角） |
| GET | `/order/{orderNo}` | 订单详情（仅买家本人可见） |
| POST | `/order/{orderNo}/pay` | 支付订单（**模拟**） |
| POST | `/order/{orderNo}/cancel` | 取消订单（商品回到在售） |
| POST | `/order/{orderNo}/confirm` | 确认收货 |

### 失效商品的处理

购物车里的商品可能在加购之后被下架、被别人买走，甚至是自己发布的。
这些条目不会等到点结算才报错：列表里就置灰并写明原因（「商品已售出」「这是你自己发布的商品」
「商品已被删除」），复选框自动禁用，并提供「清理失效商品」一键清掉。
收藏列表同理，失效的收藏会禁用「加入购物车」。

## 💬 私信与商品咨询

两者是同一条消息链路的两种用法，共用 `private_message` 一张表：

| 能力 | 入口 | 是否带商品上下文 |
|---|---|---|
| **商品咨询** | 商品详情页「我要购买」 | 带，会话里会显示被咨询的商品 |
| **私信** | 商品详情页「私信联系」、头部「私信」菜单 | 不带，纯用户间私聊 |

- **会话归属**：会话键由「双方用户 ID 排序后拼接 + 商品 ID」生成，因此
  「a 找 b 问商品 1」「b 找 a 问商品 1」「a 和 b 的普通私聊」是三个互不混淆的会话；
  也正因为只有会话双方能算出同一个键，天然杜绝越权读取他人会话。
- **消息中心**：头部「私信」菜单带未读角标，点进去是「左侧会话列表 + 右侧聊天窗口」。
  打开某个会话即把对方发来的消息标记为已读（同时刷新角标）。
- **实时性**：项目未引入 WebSocket，聊天页与角标用轮询刷新（聊天页 10 秒、角标 30 秒），
  发消息与读消息是即时的；需要真正推送时可在此基础上替换为 SSE 或 WebSocket。

主要接口（全部需要登录，请求方身份取自登录态，不接受前端传入的发送者 ID）：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/message/private/send` | 发送私信/咨询，带 `productId` 即为咨询 |
| GET | `/message/private/conversations` | 会话列表（对方昵称头像、关联商品、未读条数） |
| GET | `/message/private/chat?peerId=&productId=` | 聊天记录，顺带标记已读 |
| GET | `/message/private/unread/count` | 未读总数（头部角标） |
| DELETE | `/message/private/chat?peerId=&productId=` | 删除会话（双方均不再可见） |

## 📊 压力测试

使用内置的压力测试脚本（位于 `tests/`）：

```bash
# 基本测试（默认10线程，100请求）
node tests/stress_test.js

# 自定义参数测试
node tests/stress_test.js -t 20 -r 100 -u http://localhost:8080/api/health

# 测试登录接口
node tests/stress_test.js -t 5 -r 50 -u http://localhost:8080/api/user/login -m POST -p '{"username":"testuser1","password":"123456"}'
```

测试报告自动写入 `reports/stress/`（以脚本所在位置定位，与执行时的工作目录无关）。

## 🔧 部署脚本使用

### Linux/Mac

```bash
./scripts/deploy.sh [command]
```

命令说明：
- `build` - 构建Docker镜像
- `start` - 启动服务
- `stop` - 停止服务
- `restart` - 重启服务
- `status` - 查看服务状态
- `logs` - 查看服务日志
- `clean` - 清理Docker资源
- `health` - 健康检查
- `migrate` - 数据库迁移
- `backup` - 数据库备份

### Windows

```powershell
.\scripts\deploy.ps1 [command]
```

命令说明同上。

## 📁 项目结构

```
Haven-Store/
├── backend/                 # Spring Boot后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/   # 按业务分包：product/order/cart/message/user/admin/category/report
│   │   │   │                        #   + market（成交行情：估价/看板/趋势的数据底座）
│   │   │   │                        #   + campus（校园场景：校区与安全面交点）
│   │   │   │                        #   + search（增强搜索：补全/纠错/埋点）
│   │   │   │                        #   + recommend（场景化推荐：R1~R5 规则与互补字典）
│   │   │   └── resources/
│   │   └── test/
│   ├── sql/                 # MySQL 建表脚本（01-schema 建表+种子；02/03-upgrade 老库增量升级，均可重复执行）
│   ├── legacy/              # 已废弃 SQL 归档（勿用，见其 README）
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # Vue.js前端
│   ├── src/
│   ├── public/              # 静态资源与图片
│   ├── package.json
│   ├── nginx.conf           # 容器内 nginx（SPA 回退 + /api 反代）
│   └── Dockerfile
├── config/                  # 部署与编排配置
│   ├── docker-compose.yml   # Docker Compose配置（build 上下文指向 ../backend、../frontend）
│   └── nginx.conf           # 独立反向代理配置（可选，默认不启用）
├── docs/                    # 开发过程记录与数据库说明
├── reports/                 # 测试报告与项目文档
│   └── stress/              # 压力测试报告输出目录
├── scripts/                 # 部署与运维脚本
│   ├── deploy.sh            # Linux/Mac 部署脚本
│   ├── deploy.ps1           # Windows 部署脚本
│   ├── demo.sh
│   ├── check_git_status.bat
│   └── verify_github_upload.bat
├── tests/                   # 接口测试与压测脚本
│   ├── api_test.py
│   ├── frontend_api_test.js
│   ├── frontend_tester.js
│   ├── stress_test.js
│   ├── test_db_connection.py
│   └── mock_server.py
├── README.md
├── CONTRIBUTING.md
└── LICENSE
```

## 🛠️ 配置说明

`application.yml` 里凡是带 `${VAR:默认值}` 的配置项，都支持**环境变量注入**：
优先读环境变量，读不到才用冒号后的默认值。默认值是为本机开发提供的，
克隆下来不改任何配置就能直接跑（与下面的建表脚本、启动脚本保持一致）。

> ⚠️ **部署到公网/服务器前，必须用环境变量覆盖下表中的敏感项。**
> 否则仓库里的默认 JWT 密钥等同于公开密钥——拿到它就能伪造任意用户的 Token。

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `secondhand_market` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | `1234` | **数据库密码（部署必改）** |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `JWT_SECRET` | 内置默认串 | **Token 签名密钥（部署必改，HS256 要求 ≥ 32 字节）** |
| `JWT_EXPIRATION` | `604800000` | Token 有效期，毫秒（默认 7 天） |

用法示例：

```bash
# Linux / Mac
export DB_PASSWORD='your-strong-password'
export JWT_SECRET='a-very-long-random-secret-at-least-32-bytes'
java -jar backend/target/second-hand-market-1.0.0.jar

# Windows PowerShell
$env:DB_PASSWORD='your-strong-password'
$env:JWT_SECRET='a-very-long-random-secret-at-least-32-bytes'
java -jar backend\target\second-hand-market-1.0.0.jar

# Docker Compose：config/docker-compose.yml 的 backend.environment 里同样支持这些变量
```

### 数据库配置

对应的配置项（`backend/src/main/resources/application.yml`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:secondhand_market}?...
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:1234}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis配置

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    database: 0
    password: # 如果设置了密码
```

### JWT配置

```yaml
jwt:
  secret: "${JWT_SECRET:内置默认串}"
  expiration: ${JWT_EXPIRATION:604800000} # 默认 7 天
```

## 🚨 注意事项

1. **安全配置**
   - 部署前用环境变量覆盖 `DB_PASSWORD` 与 `JWT_SECRET`（见上表）
   - 生产环境请修改默认密码
   - 配置HTTPS证书
   - 设置强密码策略

2. **性能优化**
   - 启用Gzip压缩
   - 配置CDN加速
   - 使用数据库连接池

3. **数据备份**
   - 定期备份数据库
   - 重要数据做多副本

4. **监控**
   - 设置服务监控告警
   - 配置日志收集
   - 性能监控

## 📈 性能监控

### 应用监控

- Prometheus + Grafana
- Spring Boot Actuator
- 自定义指标

### 数据库监控

- MySQL慢查询日志
- Redis监控
- 连接池监控

## 🤝 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交代码
4. 发起Pull Request

## 📄 许可证

MIT License

## 🆘 常见问题

### 0. 页面弹「系统内部错误」（接口返回 `code: 501`）

含义：后端抛了未预期的异常，最常见的原因就是**连不上数据库**——MySQL 没启动，
所有读写库的接口（登录、商品列表、分类等）都会返回 501，而 `/api/health` 仍然正常。
因为健康检查不查库，很容易误判成「后端没问题」。

排查与修复：

```bash
# 1. 看两个依赖端口在不在监听（没有输出就是没起来）
netstat -ano | findstr ":3306"   # MySQL
netstat -ano | findstr ":6379"   # Redis

# 2. 起依赖服务（无需管理员权限）
scripts\start-dev-services.bat

# 3. 验证（应返回 {"code":200,...}）
curl http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/user/login -H "Content-Type: application/json" -d "{\"username\":\"testuser1\",\"password\":\"123456\"}"
```

MySQL 起来后后端会自动重连，**不需要重启 jar**。
若接口改报 500 且提示 `Table 'secondhand_market.user' doesn't exist`，说明库没建，
再执行一次 `scripts\init-database.bat`（或直接跑 `scripts\start-dev-services.bat`，它会自动建库）。

### 1. 端口冲突
- 修改 `config/docker-compose.yml` 中的端口映射
- 检查端口是否被占用

### 2. 数据库连接失败
- 检查MySQL服务是否启动
- 验证连接参数是否正确

### 3. Redis连接失败
- 确保Redis服务运行
- 检查密码配置

### 4. 内存不足
- 调整JVM参数
- 增加服务器内存

### 5. 502错误
- 检查后端服务是否正常
- 查看Nginx错误日志

## 📞 技术支持

如有问题，请通过以下方式联系：

- 邮箱：1593504559@qq.com
- 文档：[项目文档](docs/)

---

**祝你使用愉快！** 🎉