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
 * 收藏列表里的美术馆卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("收藏的美术馆VO")
public class FavoriteGalleryVO implements Serializable {

    @ApiModelProperty("收藏类型：1-展览 2-美术馆")
    private Integer targetType;

    @ApiModelProperty("美术馆ID")
    private Long targetId;

    @ApiModelProperty("美术馆名称")
    private String name;

    @ApiModelProperty("封面图")
    private String coverImage;

    @ApiModelProperty("地址")
    private String address;

    @ApiModelProperty("当前在展数量")
    private Integer exhibitionCount;

    @ApiModelProperty("收藏时间")
    private LocalDateTime favoriteTime;
}
