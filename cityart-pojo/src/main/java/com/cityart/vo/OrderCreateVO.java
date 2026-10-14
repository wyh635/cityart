package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单成功返回，前端拿 orderNo 跳支付页
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("下单结果VO")
public class OrderCreateVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("订单总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("状态，固定为 1-待支付")
    private Integer status;

    @ApiModelProperty("支付超时时间（下单 + 30 分钟）")
    private LocalDateTime payExpireTime;

    @ApiModelProperty("剩余支付秒数（由后端算，前端倒计时直接用）")
    private Long remainingSeconds;
}
