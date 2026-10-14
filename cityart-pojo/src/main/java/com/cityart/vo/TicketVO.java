package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 票夹里的一张电子票（一张卡 = 一个已支付订单，一单一码）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("票夹VO")
public class TicketVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("展览海报")
    private String posterImage;

    @ApiModelProperty("展览标题")
    private String exhibitionTitle;

    @ApiModelProperty("观展场馆")
    private String galleryName;

    @ApiModelProperty("观展日期")
    private LocalDate visitDate;

    @ApiModelProperty("票种名称")
    private String ticketName;

    @ApiModelProperty("共几张")
    private Integer quantity;

    @ApiModelProperty("核销二维码")
    private String qrCode;

    @ApiModelProperty("有效期")
    private LocalDateTime expireTime;

    @ApiModelProperty("还有几天过期")
    private Long remainDays;

    @ApiModelProperty("状态值")
    private Integer status;

    @ApiModelProperty("状态中文，如「有效」")
    private String statusText;
}
