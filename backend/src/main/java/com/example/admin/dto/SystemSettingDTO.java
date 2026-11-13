package com.example.admin.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * 系统设置（读写共用）
 * <p>
 * 字段结构与管理后台「系统设置」页的表单一一对应，
 * 前端拿到后直接 Object.assign 到表单模型上，因此命名不可随意调整
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "SystemSettingDTO", description = "系统设置")
public class SystemSettingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 站点设置 */
    private SiteSetting site;

    /** 上传设置 */
    private UploadSetting upload;

    /** 交易设置 */
    private TradeSetting trade;

    /** 联系方式 */
    private ContactSetting contact;

    /** 安全设置 */
    private SecuritySetting security;

    /**
     * 构建默认设置
     * <p>
     * 说明：系统尚未保存过设置时返回本默认值，保证前端表单始终有完整数据可渲染
     *
     * @return 默认系统设置
     */
    public static SystemSettingDTO defaults() {
        SystemSettingDTO dto = new SystemSettingDTO();

        SiteSetting site = new SiteSetting();
        site.setName("闲置港湾 Haven-Store");
        site.setDescription("面向校园与同城的闲置循环平台——用真实成交行情定对价，用校区与面交点让面交更省心");
        site.setLogo("/logo.png");
        site.setIcp("京ICP备123456789号");
        dto.setSite(site);

        UploadSetting upload = new UploadSetting();
        // 单位 KB，与前端保持一致（5MB）
        upload.setMaxFileSize(5120);
        upload.setAllowedTypes(Arrays.asList("jpg", "jpeg", "png", "gif"));
        upload.setMaxImages(9);
        dto.setUpload(upload);

        TradeSetting trade = new TradeSetting();
        trade.setAutoEstimate(true);
        trade.setEstimateConfidence(80);
        trade.setMaxPrice(1000000);
        trade.setAutoOfflineHours(0);
        // 本轮新增：数据驱动定价的可选开关（缺省全开，老库旧 config_json 缺字段时同样按开启处理）
        trade.setMarketBasis(true);
        trade.setDealBadge(true);
        trade.setPriceSuggest(true);
        trade.setStaleDays(14);
        trade.setMinSampleSize(3);
        // 本轮新增：增强搜索与场景推荐的可选开关
        trade.setRecommendEnabled(true);
        trade.setSearchConditionFilter(false);
        dto.setTrade(trade);

        ContactSetting contact = new ContactSetting();
        contact.setEmail("admin@haven-store.com");
        contact.setPhone("400-123-4567");
        contact.setAddress("北京市朝阳区国贸CBD");
        contact.setWorkTime(Arrays.asList("09:00", "18:00"));
        dto.setContact(contact);

        SecuritySetting security = new SecuritySetting();
        security.setPasswordStrength("medium");
        security.setLoginAttempts(5);
        security.setLockDuration(30);
        security.setSessionTimeout(120);
        dto.setSecurity(security);

        return dto;
    }

    /** 站点设置 */
    @Data
    public static class SiteSetting implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 站点名称 */
        private String name;
        /** 站点描述 */
        private String description;
        /** 站点 Logo 地址 */
        private String logo;
        /** ICP 备案号 */
        private String icp;
    }

    /** 上传设置 */
    @Data
    public static class UploadSetting implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 单文件大小上限（KB） */
        private Integer maxFileSize;
        /** 允许的文件扩展名 */
        private List<String> allowedTypes;
        /** 单次最多上传张数 */
        private Integer maxImages;
    }

    /** 交易设置 */
    @Data
    public static class TradeSetting implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 是否开启 AI 自动估价 */
        private Boolean autoEstimate;
        /** 估价可信度阈值（百分比） */
        private Integer estimateConfidence;
        /** 商品价格上限（元） */
        private Integer maxPrice;
        /** 商品自动下架时长（小时，0 表示不自动下架） */
        private Integer autoOfflineHours;
        /** 是否启用成交行情估价（估价优先取平台成交中位价，关闭则走规则基线） */
        private Boolean marketBasis;
        /** 是否启用买家侧「划算指数」（详情页展示价格分位） */
        private Boolean dealBadge;
        /** 是否启用卖家侧「降价建议」（我的发布展示滞销降价提示） */
        private Boolean priceSuggest;
        /** 滞销判定天数（挂牌超过该天数且价格高于行情九折时提示降价） */
        private Integer staleDays;
        /** 行情最小样本量（同类成交样本低于该值时不做行情估价与建议） */
        private Integer minSampleSize;
        /** 是否启用场景化推荐（首页/搜索后/详情页三处推荐区块总开关） */
        private Boolean recommendEnabled;
        /** 是否启用搜索结果按成色二次筛选（进阶能力，缺省关闭） */
        private Boolean searchConditionFilter;
        /** 是否启用暗色模式（前台/后台三模式主题切换的总开关，缺省开启；关闭后强制亮色并隐藏切换入口） */
        private Boolean themeDarkEnabled;
        /** 是否启用私信议价（卖家会话内发报价、买家一键接受下单，缺省开启） */
        private Boolean tradeNegotiationEnabled;
        /** 是否启用面交确认码（支付后生成 6 位码，卖家输码确认交付，缺省开启） */
        private Boolean tradeConfirmCodeEnabled;
    }

    /** 联系方式 */
    @Data
    public static class ContactSetting implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 联系邮箱 */
        private String email;
        /** 联系电话 */
        private String phone;
        /** 联系地址 */
        private String address;
        /** 工作时间（起、止） */
        private List<String> workTime;
    }

    /** 安全设置 */
    @Data
    public static class SecuritySetting implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 密码强度要求：low / medium / high */
        private String passwordStrength;
        /** 登录失败次数上限 */
        private Integer loginAttempts;
        /** 账号锁定时长（分钟） */
        private Integer lockDuration;
        /** 会话超时时间（分钟） */
        private Integer sessionTimeout;
    }
}
