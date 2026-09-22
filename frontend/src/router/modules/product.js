/**
 * 商品路由配置
 */
const productRoutes = [
  {
    path: 'products',
    name: 'Products',
    component: () => import('@/views/product/ProductList.vue'),
    meta: {
      title: '商品列表',
      requiresAuth: false
    }
  },
  {
    path: 'products/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/ProductDetail.vue'),
    meta: {
      title: '商品详情',
      requiresAuth: false
    }
  },
  {
    path: 'products/create',
    name: 'ProductCreate',
    component: () => import('@/views/product/ProductCreate.vue'),
    meta: {
      title: '发布商品',
      requiresAuth: true
    }
  }
]

export default productRoutes