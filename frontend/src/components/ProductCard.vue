<template>
  <div class="product-card" @click="goDetail">
    <AppImage class="product-card__cover" :src="product.coverImage" :alt="product.title" />
    <div class="product-card__body">
      <div class="product-card__title" :title="product.title">{{ product.title }}</div>
      <div class="product-card__meta">
        <span v-if="product.categoryName" class="product-card__category">{{
          product.categoryName
        }}</span>
        <span v-if="showCondition" class="product-card__condition">{{
          product.productCondition
        }}</span>
      </div>
      <div class="product-card__footer">
        <div class="product-card__price">
          <span class="price-now">¥{{ formatPrice(product.price) }}</span>
          <span v-if="product.originalPrice" class="price-origin"
            >¥{{ formatPrice(product.originalPrice) }}</span
          >
        </div>
        <span v-if="product.username" class="product-card__seller">{{ product.username }}</span>
      </div>
      <!-- 状态角标：仅非「在售」状态展示（我的发布等场景复用） -->
      <el-tag
        v-if="statusInfo && product.status !== undefined && product.status !== 1"
        class="product-card__status"
        :type="statusInfo.type"
        size="mini"
      >
        {{ statusInfo.label }}
      </el-tag>
    </div>
  </div>
</template>

<script>
import AppImage from './AppImage.vue'
import { PRODUCT_STATUS_MAP } from '@/utils/constants'
import { formatMoney } from '@/utils/format'

/**
 * 商品卡片（全站共享）
 * <p>
 * 此前商品卡片在商品列表页与「我的发布」两处重复手写，样式与字段略有出入；
 * 统一收敛为本组件：封面（带坏链兜底）、标题、分类/成色、价格、卖家、可选状态角标。
 */
export default {
  name: 'ProductCard',
  components: { AppImage },
  props: {
    /** 商品数据（ProductListVO） */
    product: { type: Object, required: true },
    /** 是否展示成色标签 */
    showCondition: { type: Boolean, default: true }
  },
  computed: {
    statusInfo() {
      return PRODUCT_STATUS_MAP[this.product.status] || null
    }
  },
  methods: {
    formatPrice(value) {
      return formatMoney(value)
    },
    goDetail() {
      if (this.product.id) {
        this.$router.push(`/products/${this.product.id}`)
      }
    }
  }
}
</script>

<style scoped>
/* [UI美化] 商品卡：统一圆角/薄影，hover 上浮 + 主色描边 + 封面轻缩放（纯视觉微交互） */
.product-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
  border: 1px solid #ebeef5;
  position: relative;
}

.product-card:hover {
  transform: translateY(-4px);
  border-color: #d9ecff;
  /* [UI美化-本轮] hover 层次加深半档 */
  box-shadow: 0 12px 28px rgba(16, 24, 40, 0.14);
}

.product-card__cover {
  width: 100%;
  aspect-ratio: 4 / 3;
  transition: transform 0.25s ease;
}

.product-card:hover .product-card__cover {
  transform: scale(1.04);
}

.product-card__body {
  padding: 12px 14px 14px;
}

.product-card__title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 39px;
  transition: color 0.2s ease;
}

.product-card:hover .product-card__title {
  color: #409eff;
}

.product-card__meta {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

/* [UI美化] 分类/成色小标签：淡蓝底胶囊，与主色呼应 */
.product-card__category,
.product-card__condition {
  font-size: 12px;
  color: #6087a8;
  background: #f3f8fd;
  border-radius: 4px;
  padding: 2px 8px;
  line-height: 1.4;
}

.product-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 10px;
}

.price-now {
  color: #f56c6c;
  font-size: 17px;
  font-weight: 600;
  /* [UI美化-本轮] 价格数字等宽，变动时不跳动 */
  font-variant-numeric: tabular-nums;
}

.price-origin {
  color: #c0c4cc;
  font-size: 12px;
  text-decoration: line-through;
  margin-left: 6px;
}

.product-card__seller {
  font-size: 12px;
  color: #909399;
  max-width: 40%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-card__status {
  position: absolute;
  top: 8px;
  right: 8px;
}
</style>
