package com.cityart.utils;

import com.cityart.constant.ExhibitionStatusConstant;

import java.time.LocalDate;

/**
 * 展出状态工具类
 * <p>
 * 展览状态**不落库**，任何地方要用都调这里按今天实时判断，
 * 避免首页、列表页各写一份判断逻辑（改规则时只改一处）。
 */
public class ExhibitionStatusUtil {

    /**
     * 按展期和今天计算状态
     *
     * @param startDate 开展日期
     * @param endDate   闭展日期
     * @return current 正在展出 / future 即将展出 / past 往期展出
     */
    public static String of(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (startDate == null || endDate == null) {
            // 数据不完整时按「正在展出」兜底，不让前端拿到空状态
            return ExhibitionStatusConstant.CURRENT;
        }
        if (startDate.isAfter(today)) {
            return ExhibitionStatusConstant.FUTURE;
        }
        if (endDate.isBefore(today)) {
            return ExhibitionStatusConstant.PAST;
        }
        return ExhibitionStatusConstant.CURRENT;
    }
}
