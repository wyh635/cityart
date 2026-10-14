package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 「美术馆」列表分页查询参数
 */
@Data
@ApiModel("美术馆分页查询DTO")
public class GalleryPageQueryDTO implements Serializable {

    @Min(value = 1, message = "type 最小为 1")
    @Max(value = 5, message = "type 最大为 5")
    @ApiModelProperty("美术馆类型：1-5，不传表示全部类型")
    private Integer type;

    @Size(max = 50, message = "城市名称最多 50 个字符")
    @ApiModelProperty("城市，不传表示全部城市")
    private String city;

    @Size(max = 50, message = "搜索关键字最多 50 个字符")
    @ApiModelProperty("美术馆名称模糊搜索关键字")
    private String keyword;

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，最大 50，原型一屏 6 个")
    private Integer pageSize = 6;

    /**
     * LIMIT 的偏移量 = (页码 - 1) × 每页条数（只有 getter，前端传了也不采纳）
     */
    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 6 : pageSize;
        return (currentPage - 1) * size;
    }

    /**
     * 归一化后的每页条数，SQL 里直接用它
     */
    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 6 : pageSize;
    }
}
