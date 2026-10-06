package com.example.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报实体
 * <p>
 * 用户可对商品或评论发起举报，管理员在后台处理：
 * 驳回、下架被举报商品或隐藏被举报评论
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Data
@TableName("report")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 举报 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人用户 ID */
    private Long reporterId;

    /** 举报对象类型：product 商品 / comment 评论 */
    private String targetType;

    /** 举报对象 ID（商品 ID 或评论 ID） */
    private Long targetId;

    /** 举报原因（违规商品/涉嫌欺诈/假冒伪劣/侵权/色情低俗/其他） */
    private String reason;

    /** 补充说明 */
    private String description;

    /** 处理状态：0 待处理，1 已处理，2 已驳回 */
    private Integer status;

    /** 处理动作：takeDownProduct 下架商品 / hideComment 隐藏评论 / dismiss 驳回 */
    private String handleAction;

    /** 处理备注 */
    private String handleNote;

    /** 处理人（管理员）用户 ID */
    private Long handleAdminId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
