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
  },
  {
    // 订单详情：买家本人或订单内商品的卖家可见（后端校验）
    path: 'orders/:orderNo',
    name: 'OrderDetail',
    component: () => import('@/views/order/OrderDetail.vue'),
    meta: {
      title: '订单详情',
      requiresAuth: true
    }
  }
]

export default tradeRoutes
