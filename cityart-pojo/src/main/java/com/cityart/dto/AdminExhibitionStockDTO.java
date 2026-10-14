package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 后台「更新展览库存」入参
 * <p>
 * 两种用法：
 * <ul>
 *     <li>只传 {@code dailyStockLimit}：批量调整**今天及以后、且尚未生成的**日历库存</li>
 *     <li>传 {@code calendarAdjust}：精确调整指定日期的库存（加票 / 减票）</li>
 * </ul>
 */
@Data
@ApiModel("展览库存调整DTO")
public class AdminExhibitionStockDTO implements Serializable {

    @Min(value = 1, message = "每日票数上限最小为 1")
    @ApiModelProperty("每日票数上限，会把今天及以后的日历库存统一改成这个值")
    private Integer dailyStockLimit;

    @Valid
    @ApiModelProperty("指定日期的库存调整")
    private List<CalendarAdjust> calendarAdjust;

    @Data
    @ApiModel("日历库存调整项")
    public static class CalendarAdjust implements Serializable {

        @NotNull(message = "日期不能为空")
        @ApiModelProperty("日期 yyyy-MM-dd")
        private LocalDate date;

        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能为负")
        @ApiModelProperty("该日的总放票数（不得小于已售数量）")
        private Integer totalStock;
    }
}
