package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * B 端「订单管理」列表一行
 * <p>
 * 对应文档返回示例：订单号 / 状态 / 购票人 / 展览 / 场馆 / 票种 / 数量 / 金额 / 观展日期 / 支付时间 / 核销信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("状态：1-待支付 2-已支付 3-已取消 4-已退款 5-已完成")
    private Integer status;

    @ApiModelProperty("状态中文（后端给，前端不硬编码）")
    private String statusText;

    @ApiModelProperty("购票用户ID")
    private Long userId;

    @ApiModelProperty("购票人姓名")
    private String contactName;

    @ApiModelProperty("购票人手机号（脱敏）")
    private String contactPhone;

    @ApiModelProperty("展览ID")
    private Long exhibitionId;

    @ApiModelProperty("展览名称")
    private String exhibitionTitle;

    @ApiModelProperty("所属场馆")
    private String galleryName;

    @ApiModelProperty("票种名称")
    private String ticketName;

    @ApiModelProperty("票数")
    private Integer quantity;

    @ApiModelProperty("订单金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("观展日期")
    private LocalDate visitDate;

    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;

    @ApiModelProperty("支付时间（未支付为 null）")
    private LocalDateTime payTime;

    @ApiModelProperty("核销时间（未核销为 null）")
    private LocalDateTime useTime;

    @ApiModelProperty("核销方式：1-扫码 2-输入订单号（未核销为 null）")
    private Integer verifyType;

    @ApiModelProperty("退款状态：0-未申请 1-申请中 2-已退款")
    private Integer refundStatus;

    @ApiModelProperty("退款金额（已退款才有值）")
    private BigDecimal refundAmount;
}
