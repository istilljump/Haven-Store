package com.example.message.service;

/**
 * 交易事件通知服务（轮 8 业务迭代新增）
 * <p>
 * 补齐「交易事件 → 系统消息」断链：面向买卖双方与收藏者，定向写入
 * system_message（message_type=3 交易消息——该类型此前为空壳枚举，本服务
 * 是其唯一生产入口）。通知为拉取式：复用现有通知中心列表与顶栏未读角标，
 * 不引入 WebSocket。
 * <p>
 * 去重策略（审定项 2）：降价提醒按「接收人 + 标题 + 未读」去重——同一商品
 * 反复降价时，未读的旧降价提醒未处理则不重复打扰；售出提醒由商品锁定事件
 * 天然保证一次。
 *
 * @author ZCode
 * @date 2026/11
 */
public interface TradeNotifyService {

    /**
     * 给单个用户发交易消息（message_type=3）
     *
     * @param userId  接收人
     * @param title   标题（通知中心列表展示）
     * @param content 内容（可解释文案：发生了什么/下一步做什么）
     */
    void notifyUser(Long userId, String title, String content);

    /**
     * 给多个用户发同内容交易消息（收藏者批量提醒；逐条落库便于各自已读）
     *
     * @param userIds 接收人列表（去重、剔除空值由调用方保证）
     * @param title   标题
     * @param content 内容
     */
    void notifyUsers(java.util.List<Long> userIds, String title, String content);

    /**
     * 去重判断：该用户是否已有同标题的未读交易消息
     *
     * @param userId 接收人
     * @param title  标题
     * @return true=已存在（本次应跳过）
     */
    boolean hasUnreadNotice(Long userId, String title);
}
