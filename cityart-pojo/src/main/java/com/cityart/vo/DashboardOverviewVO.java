package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据看板顶部三张卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO implements Serializable {

    @ApiModelProperty("今日到馆总人数（已核销票数）")
    private Integer todayVisitCount;

    @ApiModelProperty("本周期售票总量")
    private Integer weekTicketCount;

    @ApiModelProperty("本周期总营业额")
    private BigDecimal weekRevenue;

    @ApiModelProperty("本次统计时间（前端「手动刷新」后显示）")
    private LocalDateTime refreshTime;
}
