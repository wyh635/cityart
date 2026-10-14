package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 展览作品集里的一件作品（详情页「作品集 / COLLECTION」卡片）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("展览作品VO")
public class ExhibitionWorkVO implements Serializable {

    @ApiModelProperty("作品ID")
    private Long id;

    @ApiModelProperty("作品名称")
    private String name;

    @ApiModelProperty("作者姓名")
    private String author;

    @ApiModelProperty("作品图片地址")
    private String image;

    @ApiModelProperty("作品简介")
    private String description;
}
