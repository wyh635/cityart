package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * B 端「核销记录」一行
 * <p>
 * 对应原型表格列：核销时间 / 订单编号 / 展览名称 / 购票人 / 核销方式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRecordVO implements Serializable {

    @ApiModelProperty("订单ID")
    private Long id;

    @ApiModelProperty("核销时间")
    private LocalDateTime useTime;

    @ApiModelProperty("订单编号")
    private String orderNo;

    @ApiModelProperty("展览名称")
    private String exhibitionTitle;

    @ApiModelProperty("购票人")
    private String contactName;

    @ApiModelProperty("票数")
    private Integer quantity;

    @ApiModelProperty("核销方式：1-扫码 2-输入订单号")
    private Integer verifyType;

    @ApiModelProperty("核销方式文案：扫码 / 输入订单号（服务端给，前端不硬编码）")
    private String verifyTypeText;

    @ApiModelProperty("核销操作员姓名")
    private String verifyAdminName;
}
