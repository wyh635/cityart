package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 取消订单
 */
@Data
@ApiModel("取消订单DTO")
public class OrderCancelDTO implements Serializable {

    @Size(max = 255, message = "取消原因最多 255 个字符")
    @ApiModelProperty("取消原因，可不传")
    private String cancelReason;
}
