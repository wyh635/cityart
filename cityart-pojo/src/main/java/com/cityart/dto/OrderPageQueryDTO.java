package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 我的订单分页查询参数（顶部 5 个 tab）
 */
@Data
@ApiModel("我的订单分页查询DTO")
public class OrderPageQueryDTO implements Serializable {

    @Min(value = 1, message = "status 只能是 1-5")
    @Max(value = 5, message = "status 只能是 1-5")
    @ApiModelProperty("不传=全部；1-待支付 2-已支付 3-已取消 4-已退款 5-已完成")
    private Integer status;

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，默认 5")
    private Integer pageSize = 5;

    /** LIMIT 偏移量（只有 getter，前端传了也不采纳） */
    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        return (currentPage - 1) * getLimit();
    }

    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 5 : pageSize;
    }
}
