<template>
  <div class="my-products-page">
    <div class="page-container">
      <div class="page-head">
        <h2>我的发布</h2>
        <el-button type="primary" size="small" @click="$router.push('/products/create')"
          >发布新商品</el-button
        >
      </div>

      <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="在售" name="1" />
        <el-tab-pane label="已售出" name="2" />
        <el-tab-pane label="已下架" name="3" />
      </el-tabs>

      <div v-loading="loading" style="min-height: 240px">
        <el-empty v-if="!loading && !products.length" description="该状态下暂无商品" />
        <div v-else class="product-grid">
          <div v-for="item in products" :key="item.id" class="grid-item">
            <ProductCard :product="item" />
            <div class="item-actions">
              <el-button
                v-if="Number(item.status) === 1"
                size="mini"
                @click="$router.push(`/products/${item.id}/edit`)"
                >编辑</el-button
              >
              <el-button
                v-if="Number(item.status) === 1"
                size="mini"
                type="warning"
                plain
                @click="offline(item)"
                >下架</el-button
              >
              <el-button
                v-if="Number(item.status) === 3"
                size="mini"
                type="success"
                plain
                @click="reshelf(item)"
                >重新上架</el-button
              >
              <el-button
                v-if="Number(item.status) === 3"
                size="mini"
                type="danger"
                plain
                @click="purge(item)"
                >删除</el-button
              >
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-row" v-if="total > query.pageSize">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next, jumper"
          @current-change="loadProducts"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProductCard from '@/components/ProductCard.vue'
import { getMyProducts, removeProduct, reshelfProduct, purgeProduct } from '@/api/product'

/**
 * 我的发布（独立管理页）
 * <p>
 * 此前「我的发布」挤在个人资料页的第三个 Tab 里：写死 50 条、没有删除与重新上架；
 * 独立成页后提供状态筛选、分页与完整生命周期操作：
 * 在售 → 编辑/下架；已下架 → 重新上架/彻底删除；已售出仅可查看。
 */
export default {
  name: 'MyProducts',
  components: { ProductCard },
  setup() {
    const activeStatus = ref('all')
    const products = ref([])
    const total = ref(0)
    const loading = ref(false)
    const query = reactive({ page: 1, pageSize: 12 })

    const loadProducts = async () => {
      loading.value = true
      try {
        const params = {
          page: query.page,
          pageSize: query.pageSize
        }
        if (activeStatus.value !== 'all') {
          params.status = Number(activeStatus.value)
        }
        const pageData = await getMyProducts(params)
        products.value = pageData?.records || []
        total.value = Number(pageData?.total) || 0
      } finally {
        loading.value = false
      }
    }

    const handleTabChange = () => {
      query.page = 1
      loadProducts()
    }

    const offline = (item) => {
      ElMessageBox.confirm(`下架「${item.title}」后买家将无法看到，可随时重新上架。`, '下架商品', {
        type: 'warning'
      })
        .then(async () => {
          await removeProduct(item.id)
          ElMessage.success('已下架')
          await loadProducts()
        })
        .catch(() => {})
    }

    const reshelf = (item) => {
      ElMessageBox.confirm(`重新上架「${item.title}」？`, '重新上架', { type: 'info' })
        .then(async () => {
          await reshelfProduct(item.id)
          ElMessage.success('已重新上架')
          await loadProducts()
        })
        .catch(() => {})
    }

    const purge = (item) => {
      ElMessageBox.confirm(
        `彻底删除「${item.title}」？删除后不可恢复，图片、评论与收藏关系都会一并清除。`,
        '彻底删除',
        { type: 'error', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
      )
        .then(async () => {
          await purgeProduct(item.id)
          ElMessage.success('已删除')
          await loadProducts()
        })
        .catch(() => {})
    }

    onMounted(loadProducts)

    return {
      activeStatus,
      products,
      total,
      loading,
      query,
      loadProducts,
      handleTabChange,
      offline,
      reshelf,
      purge
    }
  }
}
</script>

<style scoped>
.my-products-page {
  background: #f5f7fa;
  min-height: 100%;
  padding: 20px;
}

.page-container {
  max-width: 1080px;
  margin: 0 auto;
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px 28px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.grid-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-actions {
  display: flex;
  gap: 6px;
}

.item-actions .el-button + .el-button {
  margin-left: 0;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding-top: 20px;
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
