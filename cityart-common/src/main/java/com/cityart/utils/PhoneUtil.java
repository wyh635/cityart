package com.cityart.utils;

/**
 * 手机号工具
 */
public class PhoneUtil {

    /**
     * 手机号脱敏：13813431221 → 138****1221
     * <p>
     * 个人中心、订单等接口一律只下发脱敏号，不要返回完整手机号
     */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
