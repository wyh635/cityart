package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 展览作品集分页查询参数（详情页「作品集」区块）
 * <p>
 * 原型一屏显示 3 件作品，所以 pageSize 默认给 3
 */
@Data
@ApiModel("作品集分页查询DTO")
public class ExhibitionWorkPageQueryDTO implements Serializable {

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，最大 50，默认 3")
    private Integer pageSize = 3;

    /** LIMIT 的偏移量 = (页码 - 1) × 每页条数（只有 getter，前端传了也不采纳） */
    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int size = getLimit();
        return (currentPage - 1) * size;
    }

    /** 归一化后的每页条数，SQL 里直接用它 */
    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 3 : pageSize;
    }
}
