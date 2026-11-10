package com.example.campus.service;

import com.example.campus.entity.Campus;
import com.example.campus.entity.MeetPoint;
import com.example.campus.vo.MeetPointVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 校园场景服务
 * <p>
 * 招牌能力「校园 LBS」的数据支撑：校区列表（筛选与附近默认圆心）、
 * 校区面交点列表、离参照点最近的面交点推荐
 *
 * @author ZCode
 * @date 2026/11/10
 */
public interface CampusService {

    /**
     * 查询启用状态的校区列表（按排序值升序）
     *
     * @return 校区列表
     */
    List<Campus> listEnabledCampuses();

    /**
     * 查询校区的面交点列表（按排序值升序）
     *
     * @param campusId 校区 ID
     * @return 面交点列表
     */
    List<MeetPoint> listMeetPoints(Long campusId);

    /**
     * 推荐离参照点最近的面交点
     *
     * @param campusId  校区 ID
     * @param longitude 参照点经度（通常是商品坐标或用户定位）
     * @param latitude  参照点纬度
     * @return 最近面交点（含距离，米）；该校区没有面交点时返回 null
     */
    MeetPointVO nearestMeetPoint(Long campusId, BigDecimal longitude, BigDecimal latitude);
}
