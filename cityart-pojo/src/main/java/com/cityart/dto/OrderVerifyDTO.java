package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * B 端「扫码核销」入参
 * <p>
 * 支持两种方式（原型「电子门票核销通道」左右两块）：
 * <ul>
 *     <li>verifyType=1 扫码核销 —— 传 qrCode（二维码扫出来的内容，即订单号）</li>
 *     <li>verifyType=2 输入订单号核销 —— 传 orderNo</li>
 * </ul>
 */
@Data
@ApiModel("扫码核销DTO")
public class OrderVerifyDTO implements Serializable {

    @NotNull(message = "请选择核销方式")
    @Min(value = 1, message = "核销方式只能是 1-扫码 2-输入订单号")
    @Max(value = 2, message = "核销方式只能是 1-扫码 2-输入订单号")
    @ApiModelProperty("核销方式：1-扫码核销 2-输入订单号核销")
    private Integer verifyType;

    @Size(max = 32, message = "二维码内容过长")
    @ApiModelProperty("verifyType=1 时必传：二维码扫出来的内容（当前就是订单号）")
    private String qrCode;

    @Size(max = 32, message = "订单号过长")
    @ApiModelProperty("verifyType=2 时必传：订单号")
    private String orderNo;
}
