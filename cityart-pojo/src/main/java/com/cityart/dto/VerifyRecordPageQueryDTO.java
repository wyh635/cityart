package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * B 端「核销记录」查询参数
 * <p>
 * 筛选栏：城市 / 展览类型 + 搜索核销
 */
@Data
@ApiModel("核销记录查询DTO")
public class VerifyRecordPageQueryDTO implements Serializable {

    @ApiModelProperty("城市，不传=全部")
    private String city;

    @ApiModelProperty("展览类型（1-5），不传=全部")
    private Integer type;

    @ApiModelProperty("搜索关键字：订单号 / 展览名称 / 购票人")
    private String keyword;

    @ApiModelProperty("核销方式：1-扫码 2-输入订单号，不传=全部")
    private Integer verifyType;

    @ApiModelProperty("核销开始日期")
    private LocalDate startDate;

    @ApiModelProperty("核销结束日期")
    private LocalDate endDate;

    @Min(value = 1, message = "页码最小为 1")
    @ApiModelProperty("页码")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 50, message = "每页最多 50 条")
    @ApiModelProperty("每页条数")
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
