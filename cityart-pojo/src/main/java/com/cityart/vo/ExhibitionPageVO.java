package com.cityart.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 「全部展览」列表卡片数据（正在展出 / 即将展出 / 往期展出三个区块共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("全部展览列表VO")
public class ExhibitionPageVO implements Serializable {

    @ApiModelProperty("展览ID")
    private Long id;

    @ApiModelProperty("展览标题")
    private String title;

    @ApiModelProperty("封面海报图")
    private String posterImage;

    @ApiModelProperty("所属美术馆ID")
    private Long galleryId;

    @ApiModelProperty("美术馆名称（JOIN gallery 取得）")
    private String galleryName;

    @ApiModelProperty("开展日期")
    private LocalDate startDate;

    @ApiModelProperty("闭展日期")
    private LocalDate endDate;

    @ApiModelProperty("票价（元），0 表示免费")
    private BigDecimal price;

    @ApiModelProperty("展出状态：current / future / past（由 Service 实时计算，不落库）")
    private String status;

    @ApiModelProperty("展览分类值：1-5")
    private Integer type;

    @ApiModelProperty("展览分类中文名")
    private String typeName;

    @ApiModelProperty("是否热门：0-否 1-是")
    private Integer isHot;
}
