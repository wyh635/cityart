package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 收藏列表分页查询参数（顶部「展览 / 美术馆」双 tab）
 */
@Data
@ApiModel("收藏列表分页查询DTO")
public class FavoritePageQueryDTO implements Serializable {

    @Min(value = 1, message = "targetType 只能是 1-展览 2-美术馆")
    @Max(value = 2, message = "targetType 只能是 1-展览 2-美术馆")
    @ApiModelProperty("1-收藏的展览 2-收藏的美术馆，默认 1")
    private Integer targetType = 1;

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，默认 4（原型一屏 2×2）")
    private Integer pageSize = 4;

    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        return (currentPage - 1) * getLimit();
    }

    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 4 : pageSize;
    }
}
