package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 后台统计类接口的公共查询参数（数据看板 + 票务数据报表共用）
 * <p>
 * 时间范围用 {@code rangeType} 传，**由后端算起止日期**，前端不传具体时间。
 */
@Data
@ApiModel("后台统计查询DTO")
public class AdminStatsQueryDTO implements Serializable {

    @ApiModelProperty("时间范围：today-今天 week-本周 month-本月 custom-自定义，默认 today")
    private String rangeType = "today";

    @ApiModelProperty("城市")
    private String city;

    @ApiModelProperty("展览类型（1-5）")
    private Integer type;

    @ApiModelProperty("搜索关键字：展览名称")
    private String keyword;

    @ApiModelProperty("rangeType=custom 时的开始日期")
    private LocalDate startDate;

    @ApiModelProperty("rangeType=custom 时的结束日期")
    private LocalDate endDate;

    /**
     * 下面两个由 **Service 计算后填充**，前端不要传。
     * <p>
     * 目的：把 {@code rangeType}（today/week/month/custom）解析成具体时间边界后传给 Mapper，
     * 这样 XML 里只写参数、不写日期函数，逻辑集中在 Service 一处。
     * <p>
     * 都是**左闭右开**：{@code [startTime, endTime)}
     */
    @ApiModelProperty(value = "统计开始时间（服务端填充，左闭）", hidden = true)
    private LocalDateTime startTime;

    @ApiModelProperty(value = "统计结束时间（服务端填充，右开）", hidden = true)
    private LocalDateTime endTime;

    @Min(value = 1, message = "页码最小为 1")
    @ApiModelProperty("页码")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    @ApiModelProperty("每页条数")
    private Integer pageSize = 5;

    public int getOffset() {
        int p = page == null || page < 1 ? 1 : page;
        int s = pageSize == null || pageSize < 1 ? 5 : pageSize;
        return (p - 1) * s;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 5 : Math.min(pageSize, 100);
    }
}
