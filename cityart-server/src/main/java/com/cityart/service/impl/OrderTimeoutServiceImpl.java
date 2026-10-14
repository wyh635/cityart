package com.cityart.service.impl;

import com.cityart.entity.ExhibitionOrder;
import com.cityart.mapper.ExhibitionCalendarMapper;
import com.cityart.mapper.ExhibitionOrderMapper;
import com.cityart.mapper.TicketTypeMapper;
import com.cityart.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单超时处理实现
 */
@Service
@Slf4j
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private static final String CANCEL_REASON = "支付超时自动取消";

    @Autowired
    private ExhibitionOrderMapper orderMapper;

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Autowired
    private ExhibitionCalendarMapper calendarMapper;

    /**
     * 单笔订单取消 —— 整个方法一个事务：
     * 「改状态」与「还库存」必须同时成功或同时失败，
     * 否则会出现「订单已取消但票没还」（少卖）或「票还了但订单还在待支付」（超卖）的不一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeoutOrder(ExhibitionOrder order) {
        // 条件更新：只有仍是「待支付」才改得动。
        // 若用户在这一瞬间刚好付款成功，影响行数为 0 → 返回 false，
        // 绝不能继续释放库存，否则会把已售出的票又放出去（超卖）
        int rows = orderMapper.cancelBySystem(order.getOrderNo(), CANCEL_REASON);
        if (rows == 0) {
            return false;
        }

        // 下单时扣了两处，取消必须两处都还；
        // 两个 mapper 内部都带 `sold_count >= quantity` 条件，不会把已售数扣成负数
        if (order.getTicketTypeId() != null) {
            ticketTypeMapper.increaseStock(order.getTicketTypeId(), order.getQuantity());
        }
        if (order.getExhibitionId() != null && order.getVisitDate() != null) {
            calendarMapper.increaseStock(order.getExhibitionId(), order.getVisitDate(), order.getQuantity());
        }

        log.info("【超时取消】orderNo：{}，展览：{}，票种：{}，数量：{}，观展日期：{}",
                order.getOrderNo(), order.getExhibitionId(), order.getTicketTypeId(),
                order.getQuantity(), order.getVisitDate());
        return true;
    }
}
