package com.cityart.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 日期文案工具：前端要的中文星期、中文月份都在后端拼好，前端不处理中文
 */
public class DateTextUtil {

    private static final String[] WEEK_TEXT = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /**
     * 星期文案：周四
     */
    public static String weekDayText(LocalDate date) {
        if (date == null) {
            return null;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        // DayOfWeek 的 value：1=周一 ... 7=周日
        return WEEK_TEXT[dayOfWeek.getValue() - 1];
    }

    /**
     * 月份分组文案：2026-09 → 2026年9月
     */
    public static String monthText(String yearMonth) {
        if (yearMonth == null || !yearMonth.contains("-")) {
            return yearMonth;
        }
        String[] parts = yearMonth.split("-");
        return parts[0] + "年" + Integer.parseInt(parts[1]) + "月";
    }
}
