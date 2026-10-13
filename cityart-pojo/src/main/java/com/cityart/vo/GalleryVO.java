package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 首页「热门美术馆」返回数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("热门美术馆VO")
public class GalleryVO implements Serializable {

    @ApiModelProperty("美术馆ID")
    private Long id;

    @ApiModelProperty("美术馆名称")
    private String name;

    @ApiModelProperty("封面图")
    private String coverImage;

    @ApiModelProperty("完整地址（已含城市和区县）")
    private String address;

    @ApiModelProperty("城市")
    private String city;

    @ApiModelProperty("开放时间")
    private String openTime;

    @ApiModelProperty("当前在展数量（冗余字段）")
    private Integer exhibitionCount;
}
