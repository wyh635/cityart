package com.cityart.service;

import com.cityart.entity.ExhibitionOrder;

/**
 * 订单超时处理业务（与用户主动取消区分：本接口没有用户上下文）
 */
public interface OrderTimeoutService {

    /**
     * 取消一笔超时未支付订单，并释放两个维度的库存
     * <p>
     * 独立事务：单笔失败只回滚自己，不影响批量处理里的其他订单
     *
     * @return true = 已取消并释放库存；false = 订单状态已变（已支付/已取消），跳过
     */
    boolean cancelTimeoutOrder(ExhibitionOrder order);
}
