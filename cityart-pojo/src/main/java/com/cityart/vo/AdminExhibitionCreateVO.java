package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 后台「新增展览」返回
 * <p>
 * 让运营看到自动生成的展览编号，以及按展期生成了多少行日历库存。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExhibitionCreateVO implements Serializable {

    @ApiModelProperty("新展览ID")
    private Long id;

    @ApiModelProperty("自动生成的展览编号，如 NJ007")
    private String exhibitionCode;

    @ApiModelProperty("按展期自动生成的日历天数（每日一条，库存 = 每日票数上限）")
    private Integer calendarGenerated;

    @ApiModelProperty("自动创建的票种数量")
    private Integer ticketTypeCount;
}
