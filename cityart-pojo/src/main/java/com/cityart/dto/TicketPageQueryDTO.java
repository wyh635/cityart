package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 票夹分页查询参数（原型一次只显示一张卡）
 */
@Data
@ApiModel("票夹分页查询DTO")
public class TicketPageQueryDTO implements Serializable {

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，默认 1")
    private Integer pageSize = 1;

    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        return (currentPage - 1) * getLimit();
    }

    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 1 : pageSize;
    }
}
