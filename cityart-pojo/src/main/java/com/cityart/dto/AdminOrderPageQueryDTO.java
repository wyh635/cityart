package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * B 端「订单管理」列表查询参数
 */
@Data
@ApiModel("后台订单列表查询DTO")
public class AdminOrderPageQueryDTO implements Serializable {

    @ApiModelProperty("订单号，精确查询")
    private String orderNo;

    @ApiModelProperty("购票人手机号，模糊查询")
    private String phone;

    @ApiModelProperty("购票人姓名，模糊查询")
    private String contactName;

    @ApiModelProperty("展览ID")
    private Long exhibitionId;

    @ApiModelProperty("城市")
    private String city;

    @ApiModelProperty("订单状态：1-待支付 2-已支付 3-已取消 4-已退款 5-已完成")
    private Integer status;

    @ApiModelProperty("下单开始日期")
    private LocalDate startDate;

    @ApiModelProperty("下单结束日期（含当天）")
    private LocalDate endDate;

    @Min(value = 1, message = "页码最小为 1")
    @ApiModelProperty("页码")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    @ApiModelProperty("每页条数")
    private Integer pageSize = 10;

    public int getOffset() {
        int p = page == null || page < 1 ? 1 : page;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (p - 1) * s;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }
}
