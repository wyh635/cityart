package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 票务数据报表一行（按「统计日期 + 展览」分组）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatVO implements Serializable {

    @ApiModelProperty("统计日期")
    private LocalDate statDate;

    @ApiModelProperty("展览ID")
    private Long exhibitionId;

    @ApiModelProperty("展览名称")
    private String exhibitionTitle;

    @ApiModelProperty("当日出售票数（按支付日算，status IN (2,5)）")
    private Integer soldCount;

    @ApiModelProperty("当日核销票数（按核销日算）")
    private Integer verifiedCount;

    @ApiModelProperty("当日未核销票数 = 出售 - 核销（Service 计算）")
    private Integer unverifiedCount;

    @ApiModelProperty("单日营收")
    private BigDecimal dailyRevenue;
}
