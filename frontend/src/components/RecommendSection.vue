<template>
  <section v-if="items && items.length" class="recommend-section">
    <div class="section-head">
      <h2>{{ title }}</h2>
      <span v-if="subtitle" class="recommend-subtitle">{{ subtitle }}</span>
    </div>
    <div class="recommend-grid">
      <div
        v-for="item in items"
        :key="item.productId"
        class="recommend-card"
        @click="$router.push(`/products/${item.productId}`)"
      >
        <el-image
          class="recommend-cover"
          :src="item.coverImage || '/default-product.png'"
          fit="cover"
          lazy
        >
          <template #error>
            <div class="recommend-cover-fallback">暂无图片</div>
          </template>
        </el-image>
        <!-- 可解释推荐：每张卡强制展示后端生成的推荐原因 -->
        <el-tag class="recommend-reason" size="small" type="info" effect="plain">
          推荐原因：{{ item.reason }}
        </el-tag>
        <div class="recommend-title">{{ item.title }}</div>
        <div class="recommend-meta">
          <span class="recommend-price">¥{{ item.price }}</span>
          <span class="recommend-tags">{{ item.categoryName }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
/**
 * 通用推荐区块（三个推荐位置共用）
 * <p>
 * 数据来自 /recommend/* 接口，条目已由后端排除自身/已售出并附带 reason；
 * 区块在 items 为空时整体不渲染——宁可不推荐，不做同类凑数
 */
defineProps({
  title: { type: String, default: '为你推荐' },
  subtitle: { type: String, default: '' },
  items: { type: Array, default: () => [] }
})
</script>

<style scoped>
/* [UI美化-本轮] 容器与首页区块统一加宽至 1200px */
.recommend-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 20px 24px;
}

/* [UI美化] 区块标题：主色竖条强调 + 标题/副标题层级区分 */
.section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
  padding-left: 10px;
  position: relative;
}

.section-head::before {
  content: '';
  position: absolute;
  left: 0;
  top: 3px;
  bottom: 3px;
  width: 3px;
  border-radius: 2px;
  background: #409eff;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.recommend-subtitle {
  font-size: 13px;
  color: #909399;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

/* [UI美化] 推荐卡：与商品卡统一观感（圆角/薄影/hover 上浮） */
.recommend-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #ebeef5;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
  display: flex;
  flex-direction: column;
}

.recommend-card:hover {
  border-color: #d9ecff;
  /* [UI美化-本轮] hover 层次加深半档 */
  box-shadow: 0 12px 28px rgba(16, 24, 40, 0.14);
  transform: translateY(-3px);
}

.recommend-cover {
  width: 100%;
  height: 140px;
  display: block;
}

.recommend-cover-fallback {
  width: 100%;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 13px;
}

.recommend-reason {
  margin: 10px 10px 0;
  align-self: flex-start;
  max-width: calc(100% - 20px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-title {
  padding: 8px 10px 0;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 10px 12px;
}

/* [UI美化] 价格色统一为项目价格红 */
.recommend-price {
  color: #f56c6c;
  font-weight: 600;
}

.recommend-tags {
  font-size: 12px;
  color: #909399;
}

/* [UI美化-本轮] 推荐卡首屏一次性渐入（全局 hs-fade-up keyframes，stagger 延迟见下） */
.recommend-grid > * {
  animation: hs-fade-up 0.35s ease backwards;
}
.recommend-grid > *:nth-child(2) {
  animation-delay: 0.04s;
}
.recommend-grid > *:nth-child(3) {
  animation-delay: 0.08s;
}
.recommend-grid > *:nth-child(4) {
  animation-delay: 0.12s;
}
.recommend-grid > *:nth-child(n + 5) {
  animation-delay: 0.16s;
}

@media (max-width: 768px) {
  .recommend-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
