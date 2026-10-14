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

/**
 * 美术馆详情页里的展览卡片
 * <p>
 * ⚠️ 整页都是同一个馆，所以**不返回 galleryName**，比「全部展览」的卡片少一个字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("馆内展览列表VO")
public class GalleryExhibitionVO implements Serializable {

    @ApiModelProperty("展览ID")
    private Long id;

    @ApiModelProperty("展览标题")
    private String title;

    @ApiModelProperty("封面海报图")
    private String posterImage;

    @ApiModelProperty("开展日期")
    private LocalDate startDate;

    @ApiModelProperty("闭展日期")
    private LocalDate endDate;

    @ApiModelProperty("票价（元），0 表示免费")
    private BigDecimal price;

    @ApiModelProperty("展出状态：current / future / past")
    private String status;

    @ApiModelProperty("展览分类值：1-5")
    private Integer type;

    @ApiModelProperty("展览分类中文名")
    private String typeName;
}
