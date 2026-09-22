# Haven-Store 可运行性复查报告（第二次）

复查时间：2026-09-22 15:25
仓库：https://github.com/istilljump/Haven-Store
本地路径：`C:\Users\liuqianwei\Desktop\homework\code`
远程最新提交：`6857a7c`（"fix: 修复前端构建问题，创建缺失的组件文件和路由配置"）

---

## 结论速览

| 问题 | 答案 |
|---|---|
| 本地有没有内容**没上传**到 GitHub？ | **有，而且很关键** —— 后端 3 个模块、nginx 配置、5 个文件的修复都还只在本地 |
| 别人下载后能不能跑通？ | **不能。前端构建仍然失败，后端连编译都过不去** |
| 相比上次报告有进步吗？ | 有。前端缺失的页面/路由文件已补齐并提交；但引入了新的导入错误，且后端暴露了编译问题 |

---

## 一、重点：本地有、GitHub 上没有的内容

`git fetch` 后确认本地 HEAD = 远程 main = `6857a7c`，**但工作区有 7 项未跟踪 + 6 个文件已修改未提交**，这些 GitHub 上一概没有。

### 1.1 未跟踪（从未 `git add`，不是被 .gitignore 排除）

| 路径 | 作用 | 影响 |
|---|---|---|
| `backend/.../health/controller/HealthController.java` | `/api/health` 健康检查接口 | ❗ README、`deploy.sh:172`、`backend/Dockerfile:36` 全靠它；GitHub 上没有 → 健康检查必然失败 |
| `backend/.../admin/controller/AdminController.java` | 管理后台接口（`/admin/stats`、`/admin/users`、`/admin/config`、`/admin/logs`） | ❗ 前端 7 个 admin 页面全部 404 |
| `backend/.../message/`（6 个文件） | 消息模块（发送/列表/已读/未读数） | ❗ 前端消息功能全部 404 |
| `nginx/nginx.conf` | nginx 反向代理配置 | ❗ 新版 `docker-compose.yml` 挂载的就是它 |
| `commit_notes/`（7 个 md） | 开发记录（原 `ai_commit_notes/` 改名而来） | 不影响运行 |

### 1.2 已修改但未提交（GitHub 上仍是旧版本）

| 文件 | 本地改了什么 | GitHub 上是什么 |
|---|---|---|
| `backend/Dockerfile` | 加了 `RUN mvn clean package` 构建阶段 + 安装 curl | 仍是"直接 COPY target/*.jar"的旧版 |
| `docker-compose.yml` | 修复 80 端口冲突、挂载 nginx.conf | 旧版（端口冲突、挂载不存在的 `./nginx/ssl`） |
| `deploy.sh` | 修正 jar 存在性判断逻辑 | 旧版（通配符判断恒真，永远报错退出） |
| `application.yml` | 数据库名改为 `secondhand_market`（与 compose 统一） | 旧版 `second_hand_market`（**连不上库**） |
| `application-dev.properties` | H2 库名同步 | 旧版 |
| `ai_commit_notes/`（7 个文件） | 本地已删除 | 仍然存在 |

> 另外：`README.md` 没有跟着改，第 53 行仍写 `npm run serve`（该脚本不存在），第 20 行仍写 `cd secondhand-market`（实际目录名是 `Haven-Store`）。

---

## 二、为什么还是跑不通

### 2.1 前端：构建仍然失败（已实测）

```
vite v5.4.21 building for production...
✓ 1716 modules transformed.        ← 上次只有 307，说明文件确实补上了
x Build failed in 7.88s
src/utils/request.js (8:9): "store" is not exported by "src/store/index.js"
```

`store/index.js` 已从 Vuex 改成 Pinia（`export const useUserStore` / `useConfigStore`），但 `utils/request.js:8` 还在用旧的 `import { store } from '@/store'` 命名导入。

**除此之外还有 10 处同类导入错误**（我用脚本全量比对了一遍）：

| 文件 | 问题 |
|---|---|
| `utils/request.js` | `{ store }` —— `@/store` 未导出该名字 |
| `store/index.js` | `userApi` 默认导入 —— `@/api/user` 只有具名导出 |
| `views/admin/AdminCategories.vue` | `adminApi` 默认导入 —— `@/api/admin` 无默认导出 |
| `views/admin/AdminDashboard.vue` | 同上 |
| `views/admin/AdminLogin.vue` | 同上 |
| `views/admin/AdminMessages.vue` | 同上 |
| `views/admin/AdminProducts.vue` | 同上 |
| `views/admin/AdminSettings.vue` | 同上 |
| `views/admin/AdminUsers.vue` | 同上 |
| `views/product/ProductCreate.vue` | `productApi` 默认导入 —— `@/api/product` 无默认导出 |
| `views/ProductPublish.vue` | 导入 `publishProduct`，但 `@/api/product` 只导出 `addProduct` |

三个 api 文件（`user.js` / `product.js` / `admin.js`）**全部只用具名导出，没有 `export default`**，但有 9 个文件按默认导入使用它们。

后果：`npm run build` 直接失败；`npm run dev` 虽然能启动 dev server，但浏览器会抛
`SyntaxError: The requested module doesn't provide an export named 'store'`，页面白屏。

**另外还有一个只有跑起来才会暴露的坑**：`vite.config.js` 里
```js
rewrite: (path) => path.replace(/^\/api/, '')
```
会把 `/api` 前缀**去掉**，而后端 `application.yml` 里 `context-path: /api` 需要保留它。
→ dev 模式下所有接口都会 **404**。去掉这行 `rewrite` 即可。

### 2.2 后端：编译不通过（已实测）

用 JDK 17 + Maven 3.9.16 执行 `mvn clean compile`，结果 **BUILD FAILURE，101 条错误**，涉及 5 个文件，全部是**漏写 import**：

| 文件 | 缺失的 import |
|---|---|
| `product/vo/ProductAddVO.java` | `import java.math.BigDecimal;` |
| `product/mapper/ProductMapper.java` | `import java.math.BigDecimal;`、`import java.util.List;` |
| `product/controller/ProductController.java` | `import com.baomidou.mybatisplus.extension.plugins.pagination.Page;` |
| `product/serviceImpl/ProductServiceImpl.java` | `import com.baomidou.mybatisplus.extension.plugins.pagination.Page;` |
| `admin/controller/AdminController.java` | `import java.util.List;` |

典型报错：
```
[ERROR] .../ProductAddVO.java:[33,13] 找不到符号
  符号:   类 BigDecimal
[ERROR] .../ProductMapper.java:[31,5] 找不到符号
  符号:   类 List
```

这 5 个文件在 GitHub 上与本地**完全一致**（`git diff HEAD` 为空），所以**GitHub 上的后端同样编译不过**。

### 2.3 Docker：还剩 3 处

| 位置 | 问题 |
|---|---|
| `backend/Dockerfile:26` | `COPY target/ secondhand-market-*.jar app.jar` —— **语法错误**。`COPY` 只允许"源… 目标"形式，这里被解析成两个源（`target/` 和 `secondhand-market-*.jar`），后者在构建上下文根目录不存在 → 构建失败。应改回 `COPY target/secondhand-market-*.jar app.jar` |
| `backend/Dockerfile:2` | 基础镜像 `openjdk:17-jre-slim` 已停止维护，拉取可能失败；建议换 `eclipse-temurin:17-jre` |
| `nginx/nginx.conf` + compose | nginx 配置监听 **80**，但 compose 只映射了 `443:443`（80 已注释掉），且引用了不存在的 `./nginx/ssl` 证书目录 → nginx 容器起不来或无法访问。建议直接不启用该 nginx 服务，只用 frontend 容器自带的 nginx |

### 2.4 文档仍与实际不一致

- `README.md:53` `npm run serve` → 应为 `npm run dev`（第 69 行自己写的是 `dev`，前后矛盾）
- `README.md:20` `cd secondhand-market` → 实际目录名 `Haven-Store`
- `README.md:36` 管理后台 `http://localhost/admin` → dev server 在 `3000` 端口

---

## 三、修复清单（按优先级）

### P0 — 让前端能构建（4 步）
1. `frontend/src/utils/request.js`：`import { store } from '@/store'` → 改用 Pinia，例如
   `import { useUserStore } from '@/store'`，并把 `store.getters.token` → `useUserStore().token`、`store.commit('SET_LOADING', x)` → `useUserStore().setLoading(x)`、`store.dispatch('logout')` → `useUserStore().clearUser()`
2. 三个 api 文件各加一行默认导出，例如 `src/api/user.js` 末尾加：
   `export default { register, login, getCurrentUserInfo, updateUserInfo, changePassword, uploadAvatar, getUserProducts, getUserFavorites, getUserMessages, markMessageAsRead, markAllMessagesAsRead, deleteMessage, getUserOrders, getOrderDetail, cancelOrder, confirmOrder }`
   （`product.js`、`admin.js` 同理，把各自导出的函数都列进去）
3. `src/views/ProductPublish.vue`：`import { publishProduct } from '@/api/product'` → 改成 `addProduct`
4. `vite.config.js`：删掉 `rewrite: (path) => path.replace(/^\/api/, '')` 这一行

### P0 — 让后端能编译（补齐 5 个文件的 import）
按 2.2 表格逐条加上即可。

### P1 — 把本地内容提交上去
```
git add backend/src/main/java/com/example/health backend/src/main/java/com/example/admin backend/src/main/java/com/example/message
git add nginx commit_notes
git add backend/Dockerfile backend/application-dev.properties backend/src/main/resources/application.yml deploy.sh docker-compose.yml
git add -u ai_commit_notes
git commit -m "fix: 补齐健康检查/管理/消息模块，修复后端编译与Docker配置"
git push
```
（建议同时把 `commit_notes/` 加入 `.gitignore`——那是个人开发过程记录）

### P1 — 修 Docker
5. `backend/Dockerfile:26` 改回 `COPY target/secondhand-market-*.jar app.jar`
6. 基础镜像换 `eclipse-temurin:17-jre`
7. compose 里停用 nginx 服务（或补上证书 + 端口映射）

### P2 — 一致性
8. `README.md` 修正启动命令、目录名、端口、文档地址
9. 把 `application.yml` 里的明文密码改成环境变量占位
10. 清理仓库里的 `.zcode/plans/`、`ai_commit_notes/`

---

## 四、实测证据

**前端构建：**
```
$ cd frontend && ./node_modules/.bin/vite build
✓ 1716 modules transformed.
x Build failed in 7.88s
src/utils/request.js (8:9): "store" is not exported by "src/store/index.js", imported by "src/utils/request.js".
```

**后端编译：**
```
$ mvn -o -s .tools/maven-settings.xml -f backend/pom.xml clean compile
[INFO] Compiling 55 source files to ...\backend\target\classes
[ERROR] COMPILATION ERROR :
[ERROR] .../ProductAddVO.java:[33,13] 找不到符号  符号: 类 BigDecimal
...
[INFO] BUILD FAILURE
```

> 说明：为做编译验证，我执行了 `mvn clean compile`，这会清空并重建 `backend/target/`（该目录是构建产物，已被 .gitignore 排除，不影响仓库）。
