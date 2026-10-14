package com.cityart.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 「美术馆」列表卡片数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("美术馆列表VO")
public class GalleryPageVO implements Serializable {

    @ApiModelProperty("美术馆ID")
    private Long id;

    @ApiModelProperty("美术馆名称")
    private String name;

    @ApiModelProperty("封面图")
    private String coverImage;

    @ApiModelProperty("完整地址（已含城市与区县）")
    private String address;

    @ApiModelProperty("城市")
    private String city;

    @ApiModelProperty("当前在展数量（冗余字段）")
    private Integer exhibitionCount;

    @ApiModelProperty("美术馆类型值：1-5")
    private Integer type;

    @ApiModelProperty("美术馆类型中文名")
    private String typeName;
}
