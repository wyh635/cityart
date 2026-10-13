package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 首页轮播图返回数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("首页轮播图VO")
public class BannerVO implements Serializable {

    @ApiModelProperty("轮播图ID")
    private Long id;

    @ApiModelProperty("轮播大图地址")
    private String imageUrl;

    @ApiModelProperty("标题（仅供后台识别，页面不展示）")
    private String title;
}
