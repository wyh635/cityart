package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * B 端「退款审核」入参
 */
@Data
@ApiModel("后台退款审核DTO")
public class AdminOrderRefundDTO implements Serializable {

    @NotNull(message = "请选择是否通过退款")
    @ApiModelProperty("是否通过：true-同意退款 false-驳回")
    private Boolean approve;

    @Size(max = 200, message = "备注最长 200 字")
    @ApiModelProperty("审核备注（记入日志；表里没有该字段）")
    private String remark;
}
