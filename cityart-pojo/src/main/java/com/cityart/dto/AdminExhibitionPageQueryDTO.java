package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 后台「展览管理 - 展览列表」查询参数
 * <p>
 * 筛选栏：城市 / 全部状态 / 古典展览类型 + 搜索展览
 */
@Data
@ApiModel("后台展览列表查询DTO")
public class AdminExhibitionPageQueryDTO implements Serializable {

    @ApiModelProperty("城市，不传=全部城市")
    private String city;

    /**
     * 库存状态筛选（不是展览的上下架状态）：
     * all-全部 / available-有票 / soldOut-已售完
     * <p>
     * 这个条件在 Service 层对结果集做，因为它依赖日历库存计算，不是数据库列
     */
    @ApiModelProperty("库存状态：all / available / soldOut，不传=all")
    private String stockStatus;

    @ApiModelProperty("展览类型（1-5），不传=全部类型")
    private Integer type;

    @ApiModelProperty("搜索关键字：展览名称 / 展览编号")
    private String keyword;

    @Min(value = 1, message = "页码最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 50, message = "每页最多 50 条")
    @ApiModelProperty("每页条数，最大 50")
    private Integer pageSize = 10;

    /** MyBatis 里直接用 #{dto.offset}，避免 XML 里写表达式 */
    public int getOffset() {
        int p = page == null || page < 1 ? 1 : page;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (p - 1) * s;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
    }
}
