package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台「展馆管理」列表一行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGalleryVO implements Serializable {

    @ApiModelProperty("展馆ID")
    private Long id;

    @ApiModelProperty("展馆编号，如 NJ-G001")
    private String galleryCode;

    @ApiModelProperty("展馆名称")
    private String name;

    @ApiModelProperty("所在城市")
    private String city;

    @ApiModelProperty("详细地址")
    private String address;

    @ApiModelProperty("C 端展示的开放时间（由 闭馆日+开馆+闭馆 自动拼）")
    private String openTime;

    @ApiModelProperty("当前在展数量")
    private Integer exhibitionCount;

    @ApiModelProperty("展馆类型值")
    private Integer type;

    @ApiModelProperty("展馆类型中文名")
    private String typeName;

    @ApiModelProperty("状态：0-正常营业 1-闭馆维护")
    private Integer status;

    @ApiModelProperty("状态文案：营业中 / 闭馆维护")
    private String statusText;

    @ApiModelProperty("封面图")
    private String coverImage;

    @ApiModelProperty("是否热门：0-否 1-是")
    private Integer isHot;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
