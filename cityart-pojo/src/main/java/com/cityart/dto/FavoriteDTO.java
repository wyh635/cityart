package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 添加 / 取消收藏
 */
@Data
@ApiModel("收藏DTO")
public class FavoriteDTO implements Serializable {

    @NotNull(message = "targetType 不能为空")
    @Min(value = 1, message = "targetType 只能是 1-展览 2-美术馆")
    @Max(value = 2, message = "targetType 只能是 1-展览 2-美术馆")
    @ApiModelProperty("1-展览 2-美术馆")
    private Integer targetType;

    @NotNull(message = "targetId 不能为空")
    @Min(value = 1, message = "targetId 必须为正整数")
    @ApiModelProperty("收藏对象ID（展览ID 或 美术馆ID）")
    private Long targetId;
}
