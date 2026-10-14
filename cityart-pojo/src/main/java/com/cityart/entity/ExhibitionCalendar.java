package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 观展日历（按天放票），对应数据库表 exhibition_calendar
 * <p>
 * ⚠️ 表里没有记录的日期就是「不发售」，前端根本不显示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionCalendar implements Serializable {

    private Long id;

    /** 所属展览ID */
    private Long exhibitionId;

    /** 可预约日期 */
    private LocalDate calendarDate;

    /** 当日总库存 */
    private Integer totalStock;

    /** 当日已售数量 */
    private Integer soldCount;

    /** 状态：0-正常放票 1-停止预约 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
