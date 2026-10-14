package com.cityart.task;

import com.cityart.entity.ExhibitionOrder;
import com.cityart.mapper.ExhibitionOrderMapper;
import com.cityart.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超时订单自动取消任务
 * <p>
 * 业务规则来自购票页「观展须知」：
 * <blockquote>订单生成后仅有 30 分钟支付时效，超时自动取消并释放库存。</blockquote>
 * <p>
 * <b>为什么必须有这个任务</b>：用户下单时已经扣减了「票种」和「日历」两个维度的库存。
 * 若用户既不付款也不主动取消，那批票就被永久占用（别人买不到），
 * 必须由定时任务把库存释放回来。
 * <p>
 * <b>本类只负责调度</b>，单笔取消的事务在 {@link OrderTimeoutService#cancelTimeoutOrder} 里，
 * 这样某一单失败不会把整批的回滚掉。
 */
@Component
@Slf4j
public class OrderTimeoutTask {

    /** 单次最多处理多少单，避免一次捞出太多、处理时间过长 */
    private static final int BATCH_SIZE = 200;

    @Autowired
    private ExhibitionOrderMapper orderMapper;

    @Autowired
    private OrderTimeoutService orderTimeoutService;

    /**
     * 每分钟扫一次超时订单
     * <p>
     * cron 六位：秒 分 时 日 月 周。「0 * * * * ?」= 每分钟第 0 秒执行。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        List<ExhibitionOrder> timeoutOrders = orderMapper.listTimeoutOrders(BATCH_SIZE);
        if (timeoutOrders.isEmpty()) {
            return;   // 无超时订单，直接返回，不打日志避免刷屏
        }

        log.info("【超时订单任务】发现 {} 笔待支付超时订单，开始处理", timeoutOrders.size());

        int success = 0;
        int skip = 0;

        for (ExhibitionOrder order : timeoutOrders) {
            try {
                if (orderTimeoutService.cancelTimeoutOrder(order)) {
                    success++;
                } else {
                    // 状态已变（用户刚支付 / 刚自己取消），跳过是正确的，不算失败
                    skip++;
                }
            } catch (Exception e) {
                log.error("【超时订单任务】订单 {} 取消失败，本轮跳过，下轮会重新捞到",
                        order.getOrderNo(), e);
            }
        }

        // 第二阶段：把本轮因异常失败的单子再捞一次立刻重试。
        // 为什么需要：单笔失败若不重试，这单要等下一分钟才被捞到，
        // 在此期间库存一直被占着。重新查询能天然过滤掉已成功的单子。
        List<ExhibitionOrder> retryList = orderMapper.listTimeoutOrders(BATCH_SIZE);
        int retrySuccess = 0;
        for (ExhibitionOrder order : retryList) {
            try {
                if (orderTimeoutService.cancelTimeoutOrder(order)) {
                    retrySuccess++;
                }
            } catch (Exception e) {
                log.error("【超时订单任务】订单 {} 重试仍失败，留待下轮", order.getOrderNo(), e);
            }
        }

        log.info("【超时订单任务】完成：首轮成功 {} 笔、跳过 {} 笔；重试成功 {} 笔",
                success, skip, retrySuccess);
    }
}
