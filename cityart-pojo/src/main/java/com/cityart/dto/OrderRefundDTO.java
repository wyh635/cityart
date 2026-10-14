package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 申请退款
 */
@Data
@ApiModel("申请退款DTO")
public class OrderRefundDTO implements Serializable {

    @Size(max = 255, message = "退款原因最多 255 个字符")
    @ApiModelProperty("退款原因，可不传")
    private String refundReason;
}
