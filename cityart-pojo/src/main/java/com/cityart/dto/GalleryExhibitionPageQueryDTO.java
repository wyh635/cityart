package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 美术馆详情页「正在展出 / 往期展出」展览列表的分页查询参数
 */
@Data
@ApiModel("馆内展览分页查询DTO")
public class GalleryExhibitionPageQueryDTO implements Serializable {

    @Pattern(regexp = "current|future|past", message = "status 只能是 current / future / past")
    @ApiModelProperty("展出状态：current 正在展出 / past 往期展出 / future 即将展出，默认 current")
    private String status = "current";

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，最大 50")
    private Integer pageSize = 10;

    /** LIMIT 的偏移量 = (页码 - 1) × 每页条数（只有 getter，前端传了也不采纳） */
    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        return (currentPage - 1) * getLimit();
    }

    /** 归一化后的每页条数，SQL 里直接用它 */
    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 10 : pageSize;
    }
}
