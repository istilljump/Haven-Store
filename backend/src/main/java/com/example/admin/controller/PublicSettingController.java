package com.example.admin.controller;

import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 前台公开配置接口
 * <p>
 * 供前端渲染站点信息与按后台设置调整行为（估价入口显隐、上传张数前端预检等），
 * 免登录访问；只暴露非敏感字段，完整设置仍只在管理后台接口中读写
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Api(tags = "前台公开配置接口")
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicSettingController {

    private final SystemSettingService systemSettingService;

    /**
     * 获取前台所需的公开配置
     *
     * @return 站点信息 + 上传限制 + 估价开关
     */
    @ApiOperation(value = "获取公开配置", notes = "无需登录；站点名称/描述/Logo、上传限制与估价/行情/划算指数/降价建议开关")
    @GetMapping("/settings")
    public Result<Map<String, Object>> getPublicSettings() {
        SystemSettingDTO settings = systemSettingService.get();
        Map<String, Object> data = new HashMap<>();
        data.put("siteName", settings.getSite() == null ? "闲置港湾 Haven-Store" : settings.getSite().getName());
        data.put("siteDescription", settings.getSite() == null ? null : settings.getSite().getDescription());
        data.put("siteLogo", settings.getSite() == null ? null : settings.getSite().getLogo());
        if (settings.getUpload() != null) {
            data.put("uploadMaxFileSizeKB", settings.getUpload().getMaxFileSize());
            data.put("uploadMaxImages", settings.getUpload().getMaxImages());
        }
        if (settings.getTrade() != null) {
            data.put("autoEstimate", !Boolean.FALSE.equals(settings.getTrade().getAutoEstimate()));
            // 数据驱动定价的可选开关：前端据此决定是否展示行情卡与划算指数（缺省开启）
            data.put("marketBasis", !Boolean.FALSE.equals(settings.getTrade().getMarketBasis()));
            data.put("dealBadge", !Boolean.FALSE.equals(settings.getTrade().getDealBadge()));
            data.put("priceSuggest", !Boolean.FALSE.equals(settings.getTrade().getPriceSuggest()));
        }
        return Result.success(data);
    }
}
