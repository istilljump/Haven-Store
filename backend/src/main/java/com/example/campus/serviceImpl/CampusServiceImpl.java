package com.example.campus.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campus.entity.Campus;
import com.example.campus.entity.MeetPoint;
import com.example.campus.mapper.CampusMapper;
import com.example.campus.mapper.MeetPointMapper;
import com.example.campus.service.CampusService;
import com.example.campus.vo.MeetPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校园场景服务实现类
 * <p>
 * 校区与面交点都是种子化的小数据集（个位数量级），直接查库即可，
 * 不额外引入缓存与驱逐逻辑，保持模块最小闭环
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusServiceImpl implements CampusService {

    /** 校区启用状态（与 campus.status 字段对应） */
    private static final int CAMPUS_STATUS_ENABLE = 1;

    /** 地球平均半径（公里），Haversine 公式用 */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /** 校区数据访问对象 */
    private final CampusMapper campusMapper;

    /** 面交点数据访问对象 */
    private final MeetPointMapper meetPointMapper;

    @Override
    public List<Campus> listEnabledCampuses() {
        return campusMapper.selectList(new LambdaQueryWrapper<Campus>()
                .eq(Campus::getStatus, CAMPUS_STATUS_ENABLE)
                .orderByAsc(Campus::getSort)
                .orderByAsc(Campus::getId));
    }

    @Override
    public List<MeetPoint> listMeetPoints(Long campusId) {
        if (campusId == null) {
            return java.util.Collections.emptyList();
        }
        return meetPointMapper.selectList(new LambdaQueryWrapper<MeetPoint>()
                .eq(MeetPoint::getCampusId, campusId)
                .orderByAsc(MeetPoint::getSort)
                .orderByAsc(MeetPoint::getId));
    }

    @Override
    public MeetPointVO nearestMeetPoint(Long campusId, BigDecimal longitude, BigDecimal latitude) {
        if (campusId == null || longitude == null || latitude == null) {
            return null;
        }
        List<MeetPoint> points = listMeetPoints(campusId).stream()
                .filter(p -> p.getLongitude() != null && p.getLatitude() != null)
                .collect(Collectors.toList());
        if (points.isEmpty()) {
            return null;
        }
        // 面交点个位数量级，直接内存算距离取最小，无需 SQL 地理函数
        MeetPoint nearest = points.stream()
                .min(Comparator.comparingDouble(p ->
                        haversineKm(latitude.doubleValue(), longitude.doubleValue(),
                                p.getLatitude().doubleValue(), p.getLongitude().doubleValue())))
                .orElse(null);
        if (nearest == null) {
            return null;
        }
        MeetPointVO vo = new MeetPointVO();
        vo.setId(nearest.getId());
        vo.setCampusId(nearest.getCampusId());
        vo.setName(nearest.getName());
        vo.setLongitude(nearest.getLongitude());
        vo.setLatitude(nearest.getLatitude());
        vo.setDescription(nearest.getDescription());
        vo.setSort(nearest.getSort());
        double km = haversineKm(latitude.doubleValue(), longitude.doubleValue(),
                nearest.getLatitude().doubleValue(), nearest.getLongitude().doubleValue());
        vo.setDistanceMeters((int) Math.round(km * 1000));
        return vo;
    }

    /**
     * Haversine 球面距离（公里），与商品「附近搜索」使用同一套地球半径口径
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
