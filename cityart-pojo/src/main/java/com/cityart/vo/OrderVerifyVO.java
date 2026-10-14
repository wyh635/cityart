package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 扫码核销成功后返回的信息
 * <p>
 * 给核销员看一眼「核销的是谁、几点的票、几张」，确认没核错人
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVerifyVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("展览名称")
    private String exhibitionTitle;

    @ApiModelProperty("美术馆名称（观展场馆）")
    private String galleryName;

    @ApiModelProperty("票种名称")
    private String ticketName;

    @ApiModelProperty("票数")
    private Integer quantity;

    @ApiModelProperty("购票人")
    private String contactName;

    @ApiModelProperty("预约观展日期")
    private LocalDate visitDate;

    @ApiModelProperty("核销时间")
    private LocalDateTime useTime;

    @ApiModelProperty("核销方式：1-扫码 2-输入订单号")
    private Integer verifyType;

    @ApiModelProperty("当前用户的观展次数（核销后 +1）")
    private Integer visitCount;
}
