package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 后台「所属展馆」下拉项（新增展览页用）
 * <p>
 * 只返回下拉必需的三个字段，不带地址、简介等大字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryOptionVO implements Serializable {

    @ApiModelProperty("展馆ID")
    private Long id;

    @ApiModelProperty("展馆名称")
    private String name;

    @ApiModelProperty("所在城市")
    private String city;
}
