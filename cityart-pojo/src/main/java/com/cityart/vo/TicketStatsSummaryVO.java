package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 票务数据页底部三张卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsSummaryVO implements Serializable {

    @ApiModelProperty("总售票数")
    private Integer totalSoldCount;

    @ApiModelProperty("总核销数")
    private Integer totalVerifiedCount;

    @ApiModelProperty("今日总营业额")
    private BigDecimal todayRevenue;
}
