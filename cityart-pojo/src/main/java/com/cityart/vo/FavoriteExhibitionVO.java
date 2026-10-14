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
 * 收藏列表里的展览卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("收藏的展览VO")
public class FavoriteExhibitionVO implements Serializable {

    @ApiModelProperty("收藏类型：1-展览 2-美术馆")
    private Integer targetType;

    @ApiModelProperty("展览ID")
    private Long targetId;

    @ApiModelProperty("展览标题")
    private String title;

    @ApiModelProperty("海报")
    private String posterImage;

    @ApiModelProperty("美术馆名称")
    private String galleryName;

    @ApiModelProperty("开展日期")
    private LocalDate startDate;

    @ApiModelProperty("闭展日期")
    private LocalDate endDate;

    @ApiModelProperty("票价")
    private BigDecimal price;

    @ApiModelProperty("收藏时间")
    private LocalDateTime favoriteTime;
}
