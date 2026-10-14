package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 看展记录分页查询参数（已核销订单，按月分组）
 */
@Data
@ApiModel("看展记录分页查询DTO")
public class RecordPageQueryDTO implements Serializable {

    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month 格式应为 yyyy-MM")
    @ApiModelProperty("按月筛选 yyyy-MM，不传返回全部")
    private String month;

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，默认 10")
    private Integer pageSize = 10;

    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        return (currentPage - 1) * getLimit();
    }

    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 10 : pageSize;
    }
}
