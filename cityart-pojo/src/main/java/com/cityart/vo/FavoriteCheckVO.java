package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 收藏状态（详情页判断按钮显示「收藏」还是「已收藏」）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("收藏状态VO")
public class FavoriteCheckVO implements Serializable {

    @ApiModelProperty("是否已收藏")
    private Boolean favorited;
}
