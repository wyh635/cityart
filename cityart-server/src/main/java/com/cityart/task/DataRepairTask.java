package com.cityart.task;

import com.cityart.service.DataRepairService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 冗余字段校准任务
 * <p>
 * <b>为什么需要它</b>：项目里有 6 个冗余字段靠代码同步维护，但有几类变化
 * **没有任何人触发同步**，时间一长就会漂移：
 * <ul>
 *     <li>展览**自然到期**（日期过了）—— 没有接口调用，gallery.exhibition_count 停在旧值</li>
 *     <li>日历行自然过期 —— 同理</li>
 * </ul>
 * 与其在每个可能的地方都补同步逻辑（容易漏），不如每天兜底重算一次。
 * 校准 SQL 幂等，只更新对不上的行，正常情况下影响行数为 0，开销极小。
 */
@Component
@Slf4j
public class DataRepairTask {

    @Autowired
    private DataRepairService dataRepairService;

    /**
     * 每天凌晨 3:10 校准一次
     * <p>
     * 选凌晨是因为要扫全表（虽然带 where 条件，但仍是全表扫描），
     * 避开白天访问高峰。
     */
    @Scheduled(cron = "0 10 3 * * ?")
    public void repairDenormalizedFields() {
        long start = System.currentTimeMillis();
        try {
            dataRepairService.repairAll();
            log.info("【数据校准任务】执行完毕，耗时 {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 校准失败不影响业务，记日志即可，明天还会再跑
            log.error("【数据校准任务】执行失败", e);
        }
    }
}
