package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据看板「展览排行」一行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionRankVO implements Serializable {

    @ApiModelProperty("排名（Service 按分页计算，不是数据库列）")
    private Integer rank;

    @ApiModelProperty("展览ID")
    private Long exhibitionId;

    @ApiModelProperty("展览名称")
    private String exhibitionName;

    @ApiModelProperty("累计售票数")
    private Integer ticketCount;

    @ApiModelProperty("客流占比（%）= 该展览票数 / 全部售票总数 × 100，保留 1 位小数")
    private Double visitorRatio;
}
