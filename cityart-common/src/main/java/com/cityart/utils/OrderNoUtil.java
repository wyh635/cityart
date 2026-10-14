package com.cityart.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单号生成工具
 * <p>
 * ⚠️ 订单号**不能用数据库自增 id**（会泄露单量，也容易被猜），
 * 这里用 `yyyyMMdd` + 6 位递增序号，形如 20261001000001，长度 14 ≤ 32，
 * 唯一性最终由 exhibition_order 表的 uk_order_no 唯一索引兜底。
 */
public class OrderNoUtil {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 当天的序号：进程内递增，初始值取时间戳尾数，避免重启后从 0 开始撞号 */
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1000000);

    public static String generate() {
        return generate(LocalDate.now());
    }

    public static String generate(LocalDate date) {
        long seq = SEQUENCE.incrementAndGet() % 1000000;
        return date.format(DAY_FORMAT) + String.format("%06d", seq);
    }
}
