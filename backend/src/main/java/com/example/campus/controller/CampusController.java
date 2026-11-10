package com.example.campus.controller;

import com.example.campus.entity.Campus;
import com.example.campus.entity.MeetPoint;
import com.example.campus.service.CampusService;
import com.example.campus.vo.MeetPointVO;
import com.example.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 校园场景模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如：GET /api/campus/list；
 * 校区与面交点是公开的场景数据（不涉及用户隐私），接口对游客开放
 * （放行配置见 application.yml 的 jwt.public-paths）
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Api(tags = "校园场景接口")
@RestController
@RequestMapping("/campus")
@RequiredArgsConstructor
public class CampusController {

    /** 校园场景服务对象 */
    private final CampusService campusService;

    /**
     * 查询启用状态的校区列表
     *
     * @return 校区列表（含中心坐标，附近模式可直接取作圆心）
     */
    @ApiOperation(value = "获取校区列表", notes = "无需登录；返回启用状态的校区及其中心坐标")
    @GetMapping("/list")
    public Result<List<Campus>> list() {
        return Result.success(campusService.listEnabledCampuses());
    }

    /**
     * 查询校区的面交点列表
     *
     * @param campusId 校区 ID
     * @return 面交点列表
     */
    @ApiOperation(value = "获取校区面交点", notes = "无需登录；返回校区内推荐的安全面交位置")
    @GetMapping("/{campusId}/meet-points")
    public Result<List<MeetPoint>> meetPoints(@PathVariable Long campusId) {
        return Result.success(campusService.listMeetPoints(campusId));
    }

    /**
     * 推荐离参照点最近的面交点
     *
     * @param campusId  校区 ID
     * @param longitude 参照点经度（通常是商品坐标）
     * @param latitude  参照点纬度
     * @return 最近面交点（含距离，米）
     */
    @ApiOperation(value = "推荐最近面交点", notes = "无需登录；按 Haversine 球面距离返回离参照点最近的安全面交位置")
    @GetMapping("/nearest-meet-point")
    public Result<MeetPointVO> nearestMeetPoint(@RequestParam Long campusId,
                                                @RequestParam BigDecimal longitude,
                                                @RequestParam BigDecimal latitude) {
        return Result.success(campusService.nearestMeetPoint(campusId, longitude, latitude));
    }
}
