package com.example.product.service;

import com.example.product.vo.CreditVO;

/**
 * 卖家信用画像服务（轮4：可信交易轻量层）
 * <p>
 * 放在 product 模块：信用画像需要读订单/评论/举报数据做实时统计，
 * product→order/report 为单向依赖，无循环；user 模块只读信用结果，
 * 信用分随交易即时变化——与行情同源的飞轮逻辑，确定性公式可断言
 *
 * @author ZCode
 * @date 2026/11/11
 */
public interface CreditService {

    /**
     * 计算卖家信用画像
     * <p>
     * 信用分公式（确定性，同数据同结果）：
     * base 60 + 成交件数×3 +（评价均分-3）×10 - 有效举报×5，夹取到 [0, 100]；
     * 无成交返回基线 60 分（level=待积累）
     *
     * @param userId 卖家用户 ID
     * @return 信用画像；用户不存在仍返回基线画像（不抛错，详情页可用）
     */
    CreditVO getCredit(Long userId);
}
