package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付结果（前端轮询用，「支付成功！」「支付失败」两个弹窗看 paid）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("支付结果VO")
public class PayResultVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("状态：1-待支付 2-已支付 3-已取消 4-已退款 5-已完成")
    private Integer status;

    @ApiModelProperty("状态中文")
    private String statusText;

    @ApiModelProperty("是否支付成功")
    private Boolean paid;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("核销二维码")
    private String qrCode;

    @ApiModelProperty("票有效期")
    private LocalDateTime expireTime;
}
