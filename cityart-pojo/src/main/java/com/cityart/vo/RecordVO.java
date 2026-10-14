package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 看展记录里的一条（一个已核销订单，按月分组）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("看展记录VO")
public class RecordVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("月份分组，如 2026-09（前端按它分组）")
    private String monthGroup;

    @ApiModelProperty("月份中文，如「2026年9月」")
    private String monthText;

    @ApiModelProperty("入场时间")
    private LocalDateTime useTime;

    @ApiModelProperty("展览海报")
    private String posterImage;

    @ApiModelProperty("展览标题")
    private String exhibitionTitle;

    @ApiModelProperty("观展场馆")
    private String galleryName;

    @ApiModelProperty("票种名称")
    private String ticketName;

    @ApiModelProperty("数量")
    private Integer quantity;
}
