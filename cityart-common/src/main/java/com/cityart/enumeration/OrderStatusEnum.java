package com.cityart.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 * <p>
 * 中文名（statusText）由后端统一给，前端不硬编码状态映射。
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    WAIT_PAY(1, "待支付"),
    PAID(2, "已支付"),
    CANCELED(3, "已取消"),
    REFUNDED(4, "已退款"),
    FINISHED(5, "已完成");

    /** 存库的状态值 */
    private final Integer value;

    /** 前端角标上的中文 */
    private final String text;

    /**
     * 根据状态值取中文，取不到返回 null
     */
    public static String textOf(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item.text;
            }
        }
        return null;
    }
}
