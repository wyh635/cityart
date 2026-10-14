package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 可预约观展日期（购票页「选择观展日期 DATE」）
 * <p>
 * ⚠️ 只返回日历表里有记录的日期，没记录的日期就是「不发售」
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("观展日期VO")
public class CalendarVO implements Serializable {

    @ApiModelProperty("日期 yyyy-MM-dd")
    private LocalDate date;

    @ApiModelProperty("星期，如「周四」（由后端算）")
    private String weekDay;

    @ApiModelProperty("当日剩余票数")
    private Integer remaining;

    @ApiModelProperty("是否已售完，true 时前端置灰")
    private Boolean soldOut;
}
