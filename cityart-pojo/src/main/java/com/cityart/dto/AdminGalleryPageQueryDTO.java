package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 后台「展馆管理」列表查询参数
 */
@Data
@ApiModel("后台展馆列表查询DTO")
public class AdminGalleryPageQueryDTO implements Serializable {

    @ApiModelProperty("城市，不传=全部城市")
    private String city;

    @ApiModelProperty("展馆类型（1-5），不传=全部类型")
    private Integer type;

    @ApiModelProperty("搜索关键字：展馆名称 / 展馆编号")
    private String keyword;

    @ApiModelProperty("状态：0-正常营业 1-闭馆维护，不传=全部")
    private Integer status;

    @Min(value = 1, message = "页码最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 50, message = "每页最多 50 条")
    @ApiModelProperty("每页条数，最大 50")
    private Integer pageSize = 10;

    public int getOffset() {
        int p = page == null || page < 1 ? 1 : page;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (p - 1) * s;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
    }
}
