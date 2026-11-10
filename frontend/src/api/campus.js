import request from '@/utils/request'

/**
 * 校园场景 API（校区 / 面交点）
 * <p>
 * 招牌能力「校园 LBS」的前端数据源：列表页校区筛选、详情页最近面交点推荐、
 * 附近模式以校区中心为圆心，都从这里取数。后端接口对游客开放
 */

/**
 * 获取校区列表（含中心坐标，附近模式可直接取作圆心）
 * @returns {Promise} 返回启用状态的校区列表
 */
export function getCampuses() {
  return request.get('/campus/list')
}

/**
 * 获取校区的面交点列表
 * @param {number} campusId - 校区ID
 * @returns {Promise} 返回面交点列表
 */
export function getMeetPoints(campusId) {
  return request.get(`/campus/${campusId}/meet-points`)
}

/**
 * 推荐离参照点最近的面交点
 * @param {Object} params - 查询参数
 * @param {number} params.campusId - 校区ID
 * @param {number} params.longitude - 参照点经度（通常是商品坐标）
 * @param {number} params.latitude - 参照点纬度
 * @returns {Promise} 返回最近面交点（含 distanceMeters 距离，米）
 */
export function getNearestMeetPoint(params) {
  return request.get('/campus/nearest-meet-point', params)
}

export default {
  getCampuses,
  getMeetPoints,
  getNearestMeetPoint
}
