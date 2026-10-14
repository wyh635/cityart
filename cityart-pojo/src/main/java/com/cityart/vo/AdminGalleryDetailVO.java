package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 后台「展馆详情」返回（编辑回填）
 * <p>
 * 字段与 {@code AdminGallerySaveDTO} 对齐，另带回 C 端展示用的 openTime
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGalleryDetailVO implements Serializable {

    @ApiModelProperty("展馆ID")
    private Long id;

    @ApiModelProperty("展馆编号")
    private String galleryCode;

    @ApiModelProperty("展馆名称")
    private String name;

    @ApiModelProperty("所在城市")
    private String city;

    @ApiModelProperty("详细地址")
    private String address;

    @ApiModelProperty("开馆时间（如 08:30）")
    private String openHour;

    @ApiModelProperty("闭馆时间（如 18:30）")
    private String closeHour;

    @ApiModelProperty("固定闭馆日")
    private String closedDay;

    @ApiModelProperty("C 端展示的开放时间（自动拼接结果，只读）")
    private String openTime;

    @ApiModelProperty("封面图")
    private String coverImage;

    @ApiModelProperty("展馆简介")
    private String intro;

    @ApiModelProperty("展馆类型值")
    private Integer type;

    @ApiModelProperty("展馆类型中文名")
    private String typeName;

    @ApiModelProperty("当前在展数量")
    private Integer exhibitionCount;

    @ApiModelProperty("是否热门：0-否 1-是")
    private Integer isHot;

    @ApiModelProperty("状态：0-正常营业 1-闭馆维护")
    private Integer status;
}
