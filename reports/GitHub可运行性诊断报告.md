# Haven-Store 项目可运行性诊断报告

- **诊断时间**：2026-09-22
- **诊断对象**：https://github.com/istilljump/Haven-Store
- **本地路径**：`C:\Users\liuqianwei\Desktop\homework\code`
- **远程提交**：`5c44377`（与本地 `HEAD` 完全一致，`git status` 工作区干净，共 125 个已跟踪文件）

---

## 结论速览

| 你的问题 | 结论 |
|---|---|
| 本地项目有没有漏传到 GitHub？ | **源码没有漏传**（本地有 = 远程有，commit 哈希一致）。只有 3 类文件被 `.gitignore` **有意**排除。 |
| 别人下载后能在电脑上跑通吗？ | **不能。** 前端连构建都过不去（已实测复现），Docker 一键部署存在 8 处必然失败。 |
| 到底是"上传不完整"还是"项目本身不完整"？ | **是项目本身不完整。** 前端缺的那些源码文件，在你本地同样不存在——不是漏传，是没写。 |

---

## 一、上传完整性核查

已跟踪文件 125 个，本地 `HEAD` = 远程 `main` = `5c44377`，**本地与 GitHub 完全同步，没有"传丢了"这回事**。

### 1.1 被 `.gitignore` 排除、没上传的内容

| 路径 | 排除原因 | 是否影响别人运行 |
|---|---|---|
| `.tools/` | 本地工具链（apache-maven-3.9.16、Redis、MinGit、mingit.zip 38MB、OCR 输出等） | 不影响，本来就不该传 |
| `项目初步规划.pdf`（14.9MB） | 比赛原始材料 | 不影响 |
| `项目要求.png`（2.7MB） | 比赛原始材料 | 不影响 |
| `.zcode/export-log/` | AI 会话日志 | 不影响 |
| `target/`、`node_modules/` | 构建产物 | 不影响（标准做法） |

### 1.2 不该上传却上传了

- `.zcode/plans/plan-sess_6d21f9c2-....md` —— 这是 AI 对话生成的**开发计划**文件，属个人过程记录，建议删除并加入 `.gitignore`。

### 1.3 真正的问题

GitHub 上缺失的文件，在你本地**同样不存在**。所以这不是"上传不完整"，而是**项目本身没有写完**。详见下一节。

---

## 二、别人能不能跑通？——不能

### 2.1 前端：构建直接失败（已实测复现）

在本地实际执行 `vite build`，真实报错如下：

```
vite v5.4.21 building for production...
✓ 307 modules transformed.
x Build failed in 2.88s
error during build:
Could not resolve "./modules/home" from "src/router/index.js"
```

**缺失文件清单**（`src/router/index.js` 引用了它们，但文件不存在）：

| 被引用的路径 | 引用位置 | 状态 |
|---|---|---|
| `@/layouts/MainLayout.vue` | `router/index.js:8` | 不存在（只有 `AdminLayout.vue`） |
| `./modules/home` → `router/modules/home.js` | `router/index.js:11` | 不存在（`modules` 目录都没有） |
| `./modules/auth` | `router/index.js:12` | 不存在 |
| `./modules/product` | `router/index.js:13` | 不存在 |
| `./modules/user` | `router/index.js:14` | 不存在 |
| `@/views/NotFound.vue` | `router/index.js:20` | 不存在 |

前端实际存在的页面只有：`views/Home.vue`、`views/ProductPublish.vue`、`views/admin/*`（7 个）。
`src/components/` 是一个**空目录**。

也就是说：README 里承诺的**首页、登录页、注册页、商品列表页、商品详情页、聊天页**，在代码里根本没有。

**依赖与代码不一致**：

| 问题 | 证据 | 后果 |
|---|---|---|
| 缺 `vuex` 依赖 | `src/store/index.js:6` 写 `import { createStore } from 'vuex'`；但 `package.json` 没有 vuex，`node_modules/vuex` 也不存在 | 模块无法解析，构建报错 |
| 状态管理框架混用 | `main.js:2` 用 `createPinia()`；`store/index.js` 用 Vuex 的 `createStore`；`request.js` 用 Vuex 的 `store.getters` / `store.commit` | 架构冲突，逻辑跑不通 |
| 常量未导出 | `request.js:8` 导入 `ERROR_CODES, ERROR_MESSAGES`；`utils/constants.js` 里没有这两个导出 | 取到 undefined |
| mutation 未定义 | `store/index.js` 的 `loadConfig` 提交 `SET_CONFIG`，但 mutations 里没有 `SET_CONFIG` | 运行时抛错 |

> 补充：`@element-plus/icons-vue` 虽未写进 `package.json`，但它是 `element-plus` 的传递依赖，会被 npm 提升安装——**这一条不是问题**，不用管。

### 2.2 后端：能编译，但功能是个半成品

- 只有两个 Controller：`UserController`（register / login / info）和 `ProductController`。README 承诺的**即时聊天、系统消息、管理后台接口，后端一律没有**。
- **`/api/health` 接口不存在**（全项目检索无匹配）。而 README、`demo.sh`、`deploy.sh health`、`backend/Dockerfile` 的 HEALTHCHECK 全都依赖它 → 全部失败。
- 没有 `mvnw` / `.mvn` 包装器，使用者必须自己装 Maven。
- 版本说明互相打架：README 写 JDK 17+，`pom.xml:24` 写 `1.8`，Dockerfile 用 openjdk:17。
- **数据库名对不上**：`application.yml:23` 是 `second_hand_market`，`docker-compose.yml:36` 是 `secondhand_market`。
- `application.yml:27` 明文密码 `1234`，已提交到**公开仓库**。
- `application-dev.properties` 指向 H2 内存库，但 `pom.xml` **没有 H2 依赖** → 该配置用不了。

### 2.3 Docker 一键部署：8 处必然失败

| # | 位置 | 问题 |
|---|---|---|
| 1 | `backend/Dockerfile:11` | 直接 `COPY target/secondhand-market-*.jar`，**没有构建阶段**。而 `target/` 被 `.gitignore` 排除 → 新克隆的仓库里没有 jar → `docker build` 第一步就失败 |
| 2 | `deploy.sh:61` | `if [ ! -f "target/secondhand-market-*.jar" ]` —— **通配符被引号包住不会展开**，条件恒为真 → 即使已经打好包也会直接 `exit 1` |
| 3 | `deploy.sh:179` | `target/secondhand-market-*..jar` —— **多了一个点**，路径错误 |
| 4 | `docker-compose.yml:61` | mysql 挂载 `./backend/scripts`，**该目录不存在**（脚本实际在 `backend/sql/init.sql`）→ 建表脚本不执行 → 后端启动后所有接口报 "Table doesn't exist" |
| 5 | `docker-compose.yml:95-96` | nginx 挂载 `./nginx/nginx.conf` 和 `./nginx/ssl`，**`nginx` 目录不存在** → 挂载失败 |
| 6 | `docker-compose.yml:12` vs `:91` | frontend 和 nginx **都映射宿主 80 端口** → 端口冲突，必有一个起不来 |
| 7 | `frontend/Dockerfile:12` | `npm ci --only=production` 会跳过 devDependencies，而 **vite 就在 devDependencies 里** → `npm run build` 报 "vite: not found" |
| 8 | `backend/Dockerfile:2,20` | 基础镜像 `openjdk:17-jre-slim` 已停止维护（Docker Hub 拉取会失败）；且 slim 镜像**不带 curl**，HEALTHCHECK 必然 unhealthy |

### 2.4 Python 脚本

- `mock_server.py`、`api_test.py`、`test_db_connection.py`、`backend/run_server.py` 需要 flask / bcrypt / requests，但仓库里**没有 `requirements.txt`**。
- `demo.sh` 让人 `cd backend && python run_server.py` 来"启动后端"——那是个 Flask **假后端**，不是真正的 Spring Boot 服务。

### 2.5 文档自相矛盾

| 文档 | 说法 | 实际 |
|---|---|---|
| `README.md:3` | Spring Boot + Vue.js | 而 `项目完成报告.md:5` 写的是 Vue.js + Flask |
| `README.md:53` | `npm run serve` | `package.json` 里没有 serve，只有 dev / build / preview |
| `README.md:20` | `cd secondhand-market` | 仓库目录名是 Haven-Store |
| `README.md:145` | 包路径 `com/market/` | 实际是 `com/example/` |
| `README.md:36` | 管理后台 `http://localhost/admin` | dev server 跑在 3000 端口 |
| `README.md:89` | 文档地址 `swagger-ui/index.html` | 用的是 knife4j，实际入口是 `/doc.html` |
| `项目完成报告.md:38` | "前端项目成功启动……所有页面加载正常" | 与当前代码实际状态不符 |

---

## 三、修复优先级清单

### P0 —— 不做就跑不起来

1. 补齐前端缺失文件：`layouts/MainLayout.vue`、`views/NotFound.vue`、`router/modules/{home,auth,product,user}.js`；或简化 `router/index.js`，只保留确实存在的页面。
2. 二选一：给 `package.json` 加 `vuex` 依赖，**或**把 `store/index.js` 改写为 Pinia 并同步修改 `request.js`。
3. 在 `utils/constants.js` 里补上 `ERROR_CODES` / `ERROR_MESSAGES` 导出。
4. `package.json` 补 `"serve": "vite"` 脚本（或把 README 改成 `npm run dev`）。

### P1 —— 让 Docker 部署能通

5. `deploy.sh:61` 去掉通配符外的引号；`deploy.sh:179` 修掉 `..`。
6. 统一数据库名为 `second_hand_market`。
7. mysql 挂载路径改为 `./backend/sql`，并确认 `init.sql` 是 MySQL 语法且含 `CREATE DATABASE`。
8. `frontend/Dockerfile` 的 `npm ci` 去掉 `--only=production`；后端基础镜像换 `eclipse-temurin:17-jre`，HEALTHCHECK 改用 `wget` 或直接删掉。
9. 删除 `docker-compose.yml` 里没用的 nginx 服务，或补上 `nginx/nginx.conf` 与证书。

### P2 —— 一致性与体验

10. 全项目统一 Java 版本描述（建议对齐 `pom.xml` 的 1.8，README 改为 JDK 8+）。
11. 补一个 `/api/health` 接口（一个简单的 `@GetMapping("/health")` 即可）。
12. 新增 `requirements.txt`。
13. 修正 README 的目录名、端口、API 文档地址。
14. 删除 `.zcode/plans/` 并加入 `.gitignore`。
15. `application.yml` 的明文密码改为环境变量占位。

---

## 四、一句话总结

**代码确实全部传上去了，但没有传丢是一回事，能不能跑是另一回事——当前仓库处于"前端缺文件构建失败 + Docker 部署多处硬伤 + 后端缺接口"的状态，别人克隆下来是跑不起来的。**
