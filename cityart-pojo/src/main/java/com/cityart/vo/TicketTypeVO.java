package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 票种（购票页「选择票种 TYPE」）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("票种VO")
public class TicketTypeVO implements Serializable {

    @ApiModelProperty("票种ID，下单时传 ticketTypeId")
    private Long id;

    @ApiModelProperty("票种名称")
    private String name;

    @ApiModelProperty("该票种单价")
    private BigDecimal price;

    @ApiModelProperty("剩余库存 = total_stock - sold_count，0 时前端置灰")
    private Integer remaining;

    @ApiModelProperty("每人限购数量")
    private Integer limitPerUser;
}
