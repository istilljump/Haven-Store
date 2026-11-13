# 014 · Haven-Store 设计系统规范（UI 轮 1-7 沉淀）

> 适用范围：`frontend/src` 全部界面代码。本册是设计 Token、暗色模式、品牌资产三层体系的唯一权威说明；与代码同源，改 Token 必须同步改本册。
> 基线：轮 6 `e4c4730`（暗色体系）+ 轮 7（后台总开关 / echarts 暗色注入 / 文档化）。

---

## 一、三层 Token 架构

```
基础品牌 Token（--brand-*，brand-tokens.css，挂 html[data-brand] 作用域）
        ↓ 引用
业务定制 Token（--hs-*，theme.css 定义语义，dark.css 定义暗色值）
        ↓ 消费
全站样式（30+ .vue scoped 样式 + Element Plus --el-* 精调）
```

**使用约束（红线）**

1. 业务样式只允许消费 `--hs-*` 与 `--el-*`，禁止写死色值；
2. `--brand-*` 仅允许在 Token 层内部被引用；
3. 装饰元素（圆点徽标/圆环）规格由 Token 控制，禁止内联覆盖；
4. 新增颜色一律先进 `brand-tokens.css`（品牌资产）或 `theme.css/dark.css`（界面语义），再消费。

## 二、品牌资产 Token 清单（brand-tokens.css）

| 类别 | Token | 亮色值 | 暗色值 |
|---|---|---|---|
| 主色板 | `--brand-primary-50/100/200/300/400/500/600/700/800/900` | `#ecf5ff → #1d4ed8`（500=#409eff） | 400/500/800/900 提亮半档 |
| 功能色 | `--brand-success / warning / danger / info` | `#67c23a / #e6a23c / #f56c6c / #909399` | 同亮色（消费处自行提亮） |
| 字体家族 | `--brand-font-family` | PingFang SC / Microsoft YaHei 栈 | 同 |
| 字重阶梯 | `--brand-font-weight-regular/medium/semibold/bold` | 400/500/600/700 | 同 |
| 间距栅格 | `--brand-space-1…10` | 4/8/12/16/20/24/32/40px | 同 |
| 圆角阶梯 | `--brand-radius-sm/md/lg/xl/2xl/round` | 4/6/8/10/12/999px | 同 |
| 阴影阶梯 | `--brand-shadow-1/2/3` | 贴地→浮起→弹窗 | 透明度加深补偿 |
| LOGO 圆点 | `--brand-logo-dot-size/gap/inner/shadow/muted-opacity` | 12px 径向渐变+柔影 | 无投影、提亮渐变 |
| 装饰圆环 | `--brand-deco-ring-size(-lg)/-color` | 220/260px 白 16% | 暗蓝 14% |
| 品牌渐变 | `--brand-hero-gradient / -auth-gradient` | `#409eff→#2563eb` | 夜色 `#16304f→#0f2038` |

## 三、界面语义 Token（--hs-*，theme.css / dark.css）

| Token | 亮色 | 暗色 | 用途 |
|---|---|---|---|
| `--hs-text-primary / regular / secondary / placeholder` | `#303133 / #606266 / #909399 / #c0c4cc` | `#e6ebf4 / #c3cad6 / #8d97a8 / #5e6879` | 文字四级 |
| `--hs-border / -light` | `#ebeef5 / #f0f2f7` | `#2a3442 / #222b37` | 描边/分隔 |
| `--hs-bg-page / card / panel / panel-2 / price-soft` | `#f5f7fa / #fff / #f8fafd / #f0f2f7 / #fef0f0` | `#0e131a / #171e28 / #1d2531 / #212a36 / rgba红12%` | 底色体系 |
| `--hs-price` | `#f56c6c` | `#f78b8b` | 价格/金额专用 |
| `--hs-radius-sm / md / lg` | 6/8/12px | 同 | 按钮/卡片/弹窗 |
| `--hs-shadow-sm / md / lg` | 引用 brand-shadow-1/2/3 | 暗色补偿 | 三级阴影 |

**暗色对比度实测**（WCAG，正文 ≥4.5）：主字 14.0 / 常规 10.2 / 辅助 5.7 / 表头 7.7 / 价格 7.2，全部达标。已知边界：占位字 2.98（禁用态）、按钮白字 2.78（EP 官方行为）。

## 四、暗色模式规范

1. **机制**：`<html>.dark` 类承载全部暗色变量（Element Plus 官方暗色表 + 本站 dark.css）；**禁止简单反色**，暗色色阶为独立设计的蓝调深色系。
2. **三模式**：`system`（跟随系统+实时监听）/ `light` / `dark`；入口为前后台顶栏图标钮（已审定转正）；`localStorage['haven_theme_mode']` 持久化。
3. **总开关**：后台「交易设置 → 外观设置 → 暗色模式」（`themeDarkEnabled`，缺省开启）。关闭后强制亮色 + 隐藏入口；前端代码级兜底开关 `THEME_FEATURE_ENABLED`（theme-mode.js）。
4. **事件**：`window` 事件 `haven-theme-change` 在每次主题应用后派发，需要按主题渲染的消费方（如 echarts）监听重绘。
5. **图表**：AdminDashboard 通过 `chartPalette()` 按主题注入图例/轴文字/分割线/饼图描边色（轮 7 授权的配置级改动）；系列色保持品牌色不变；主题切换用缓存数据重绘，不重复请求接口。
6. **禁用场景**：
   - 不要在暗色下使用大面积纯黑（#000）与纯白（#fff）文字；
   - 不要给暗色卡片加强黑投影（已由 `--brand-shadow-*` 暗色档补偿）；
   - 不要在 scoped 样式新写 `html.dark` 覆盖——先判断该值是否应 Token 化。

## 五、多品牌扩展指南（预留能力，未启管理界面）

```js
// 热替换品牌：为 <html> 设置 data-brand，并新增对应 Token 覆盖块
document.documentElement.setAttribute('data-brand', 'brandB')
```

```css
/* brand-tokens.css 内新增品牌块：只覆盖需要差异化的资产 */
html[data-brand='brandB'] {
  --brand-primary-500: #10b981;
  --brand-logo-dot-inner: radial-gradient(circle at 35% 30%, #34d399, #10b981 60%, #047857);
  --brand-hero-gradient: linear-gradient(135deg, #10b981 0%, #047857 100%);
}
```

自由度边界（已审定）：**仅色板与 LOGO/装饰形态可换**；字体家族、字重阶梯、圆角、间距栅格锁定不动。

## 六、动效规范

- 基准：`0.2s ease`；卡片 hover 上浮 2~4px + `--hs-shadow-*` 浮起档；
- 列表首屏一次性渐入 `hs-fade-up`（40ms stagger，≤5 项封顶），全局尊重 `prefers-reduced-motion`；
- 暗色下 hover 阴影统一压强半档（dark.css 第三节），避免“发光感”；
- 禁止：无限循环装饰动画、视差、3D 翻转。

## 七、验收口径（历轮沉淀）

- 双主题逐页截图（1440/375）；文字对比度按 WCAG AA（4.5）与大型字（3.0）双口径；
- 全量 .vue 的 `<template>/<script>` 与基线 diff 一致性校验（业务改动需在审定清单中显式授权）；
- `npm run build` + eslint 0 告警。

---

*维护约定：Token 变更（增/改/删）必须同步更新本册对应表格；每轮 UI 迭代在本册追加“轮次差异”小节。*
