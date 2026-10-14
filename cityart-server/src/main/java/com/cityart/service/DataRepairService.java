package com.cityart.service;

import java.util.Map;

/**
 * 冗余字段校准业务
 * <p>
 * 背景：项目里有 6 个**冗余字段**（为性能而重复存储），需要由代码同步维护：
 * {@code ticket_type.sold_count}、{@code exhibition_calendar.sold_count}、
 * {@code exhibition.sold_count/total_stock/price}、{@code gallery.exhibition_count}、
 * {@code user.visit_count}。
 * <p>
 * 早期有些代码路径漏了同步（例如展览自然到期后没人更新 gallery.exhibition_count），
 * 会造成「剩余库存」「观展次数」等展示数字与实际不符。
 * <p>
 * 本接口按「唯一可信来源」（订单表 / 票种表）重算这些字段。
 * 实现是**幂等**的：只更新对不上的行，重复执行无副作用。
 */
public interface DataRepairService {

    /**
     * 校准全部冗余字段
     *
     * @return 各字段实际被修正的行数（用于日志与人工确认）
     */
    Map<String, Integer> repairAll();
}
