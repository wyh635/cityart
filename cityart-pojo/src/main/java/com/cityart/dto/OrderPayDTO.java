package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 获取支付参数（去支付）
 */
@Data
@ApiModel("去支付DTO")
public class OrderPayDTO implements Serializable {

    @NotNull(message = "支付方式不能为空")
    @Min(value = 1, message = "payType 只能是 1-微信 2-支付宝")
    @Max(value = 2, message = "payType 只能是 1-微信 2-支付宝")
    @ApiModelProperty("1-微信支付 2-支付宝")
    private Integer payType;
}
