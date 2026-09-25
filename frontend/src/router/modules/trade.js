/**
 * 交易路由配置（购物车与订单）
 */
const tradeRoutes = [
  {
    // 购物车：内含「购物车」与「我的收藏」两个 Tab，两者可互相转移
    path: 'cart',
    name: 'Cart',
    component: () => import('@/views/cart/Cart.vue'),
    meta: {
      title: '购物车',
      requiresAuth: true
    }
  },
  {
    path: 'orders',
    name: 'Orders',
    component: () => import('@/views/order/OrderList.vue'),
    meta: {
      title: '我的订单',
      requiresAuth: true
    }
  }
]

export default tradeRoutes
