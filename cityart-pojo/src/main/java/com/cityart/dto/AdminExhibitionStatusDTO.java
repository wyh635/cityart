package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 后台「展览上下架 / 热门开关」入参
 */
@Data
@ApiModel("展览状态修改DTO")
public class AdminExhibitionStatusDTO implements Serializable {

    @Min(value = 0, message = "状态只能是 0-上架 1-下架")
    @Max(value = 1, message = "状态只能是 0-上架 1-下架")
    @ApiModelProperty("上下架状态：0-上架（C 端可见） 1-下架")
    private Integer status;

    @Min(value = 0, message = "isHot 只能是 0 或 1")
    @Max(value = 1, message = "isHot 只能是 0 或 1")
    @ApiModelProperty("是否上首页热门：0-否 1-是")
    private Integer isHot;

    @ApiModelProperty("排序权重，数字越大越靠前")
    private Integer sortOrder;
}
