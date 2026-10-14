package com.cityart.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * 我的订单列表里的一个订单卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("我的订单列表VO")
public class OrderPageItemVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("状态：1-5")
    private Integer status;

    @ApiModelProperty("状态中文，如「待支付」")
    private String statusText;

    @ApiModelProperty("展览海报")
    private String posterImage;

    @ApiModelProperty("展览标题")
    private String exhibitionTitle;

    @ApiModelProperty("美术馆名称")
    private String galleryName;

    @ApiModelProperty("观展日期")
    private LocalDate visitDate;

    @ApiModelProperty("票种名称")
    private String ticketName;

    @ApiModelProperty("数量")
    private Integer quantity;

    @ApiModelProperty("单价")
    private BigDecimal price;

    @ApiModelProperty("实付金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;

    @ApiModelProperty("剩余支付秒数（仅待支付有值）")
    private Long remainingSeconds;

    @ApiModelProperty("能否去支付：待支付且未超时")
    private Boolean canPay;

    @ApiModelProperty("能否申请退款：已支付且未核销且未过展期")
    private Boolean canRefund;

    @ApiModelProperty("能否查看电子票：已支付")
    private Boolean hasTicket;

    // ===== 下面三个只用于后端算上面三个布尔值，不给前端 =====

    @JsonIgnore
    private LocalDateTime payExpireTime;

    @JsonIgnore
    private LocalDateTime useTime;

    @JsonIgnore
    private Integer refundStatus;

    @JsonIgnore
    private LocalDate exhibitionEndDate;
}
