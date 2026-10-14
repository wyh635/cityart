package com.cityart.vo;

import io.swagger.annotations.ApiModel;
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
 * 订单详情（支付页 / 订单详情页）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单详情VO")
public class OrderDetailVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("状态：1-待支付 2-已支付 3-已取消 4-已退款 5-已完成")
    private Integer status;

    @ApiModelProperty("状态中文，如「待支付」（后端给，前端不硬编码）")
    private String statusText;

    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;

    @ApiModelProperty("支付超时时间")
    private LocalDateTime payExpireTime;

    @ApiModelProperty("剩余支付秒数（由后端算，前端不要用本地时间减）")
    private Long remainingSeconds;

    @ApiModelProperty("应付总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("票种名称")
    private String ticketName;

    @ApiModelProperty("单价")
    private BigDecimal price;

    @ApiModelProperty("数量")
    private Integer quantity;

    @ApiModelProperty("观展日期")
    private LocalDate visitDate;

    @ApiModelProperty("核销二维码（status ≥ 2 后才有值）")
    private String qrCode;

    @ApiModelProperty("票有效期")
    private LocalDateTime expireTime;

    @ApiModelProperty("核销时间（已入场才有值）")
    private LocalDateTime useTime;

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("联系手机号")
    private String contactPhone;

    @ApiModelProperty("展览ID")
    private Long exhibitionId;

    @ApiModelProperty("展览标题")
    private String exhibitionTitle;

    @ApiModelProperty("展览海报")
    private String posterImage;

    @ApiModelProperty("美术馆名称")
    private String galleryName;

    @ApiModelProperty("美术馆地址")
    private String galleryAddress;

    @ApiModelProperty("展览开始日期")
    private LocalDate exhibitionStartDate;

    @ApiModelProperty("展览结束日期")
    private LocalDate exhibitionEndDate;
}
