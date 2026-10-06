<template>
  <img
    class="app-image"
    :src="currentSrc"
    :alt="alt"
    :loading="lazy ? 'lazy' : 'eager'"
    @error="handleError"
  />
</template>

<script>
/**
 * 通用图片组件
 * <p>
 * 统一解决全站两个图片体验问题：
 * 1. 坏链兜底——加载失败时自动切换到占位图，不再出现「破图」图标；
 * 2. 懒加载——视口外的图片延迟加载，列表页首屏更快。
 */
export default {
  name: 'AppImage',
  props: {
    /** 图片地址 */
    src: { type: String, default: '' },
    /** 替代文本 */
    alt: { type: String, default: '' },
    /** 是否懒加载（默认开启） */
    lazy: { type: Boolean, default: true },
    /** 加载失败时的占位图 */
    fallback: { type: String, default: '/default-product.png' }
  },
  data() {
    return {
      // 是否已经切换到占位图（占位图自身再失败时不做无限循环）
      failed: false
    }
  },
  computed: {
    currentSrc() {
      if (!this.src || this.failed) {
        return this.fallback
      }
      return this.src
    }
  },
  watch: {
    // 切换图片地址时重置失败标记（编辑页换图、分页复用组件等场景）
    src() {
      this.failed = false
    }
  },
  methods: {
    handleError() {
      if (this.failed) return
      this.failed = true
    }
  }
}
</script>

<style scoped>
.app-image {
  display: block;
  object-fit: cover;
  background: #f5f7fa;
}
</style>
