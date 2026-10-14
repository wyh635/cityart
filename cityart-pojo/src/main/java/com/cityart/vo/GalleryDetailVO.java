package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 美术馆详情页主体数据（顶部信息 + 展馆简介）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("美术馆详情VO")
public class GalleryDetailVO implements Serializable {

    @ApiModelProperty("美术馆ID")
    private Long id;

    @ApiModelProperty("美术馆名称")
    private String name;

    @ApiModelProperty("封面图（顶部大图）")
    private String coverImage;

    @ApiModelProperty("完整地址（已含城市与区县）")
    private String address;

    @ApiModelProperty("城市")
    private String city;

    @ApiModelProperty("开放时间")
    private String openTime;

    @ApiModelProperty("展馆简介长文")
    private String intro;

    @ApiModelProperty("美术馆类型值：1-5")
    private Integer type;

    @ApiModelProperty("美术馆类型中文名")
    private String typeName;

    @ApiModelProperty("当前在展数量（冗余字段）")
    private Integer exhibitionCount;

    @ApiModelProperty("是否热门：0-否 1-是")
    private Integer isHot;

    @ApiModelProperty("当前用户是否已收藏；未登录（游客）恒为 false")
    private Boolean isFavorited;

    @ApiModelProperty("「正在展出」区块的展览总数")
    private Integer currentCount;

    @ApiModelProperty("「往期展出」区块的展览总数")
    private Integer pastCount;
}
