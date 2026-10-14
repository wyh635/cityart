package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 后台「展览详情」返回
 * <p>
 * 用于编辑展览时回填表单，所以字段与 {@code AdminExhibitionSaveDTO} 对齐，
 * 并额外带上 4 个票种明细和已生成的日历天数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExhibitionDetailVO implements Serializable {

    @ApiModelProperty("展览ID")
    private Long id;

    @ApiModelProperty("展览编号，如 NJ001")
    private String exhibitionCode;

    @ApiModelProperty("展览名称")
    private String title;

    @ApiModelProperty("所属展馆ID")
    private Long galleryId;

    @ApiModelProperty("所属展馆名称")
    private String galleryName;

    @ApiModelProperty("开展日期")
    private LocalDate startDate;

    @ApiModelProperty("闭展日期")
    private LocalDate endDate;

    @ApiModelProperty("展览分类值")
    private Integer type;

    @ApiModelProperty("展览分类中文名")
    private String typeName;

    @ApiModelProperty("展览开放时间")
    private String openTime;

    @ApiModelProperty("每日票数上限")
    private Integer dailyStockLimit;

    @ApiModelProperty("展览海报")
    private String posterImage;

    @ApiModelProperty("展览实景图，逗号分隔")
    private String images;

    @ApiModelProperty("展览简介")
    private String description;

    @ApiModelProperty("是否热门：0-否 1-是")
    private Integer isHot;

    @ApiModelProperty("排序权重")
    private Integer sortOrder;

    @ApiModelProperty("上下架状态：0-上架 1-下架")
    private Integer status;

    @ApiModelProperty("票种列表")
    private List<TicketItem> tickets;

    @ApiModelProperty("已生成的日历天数（确认「每日票数上限」是否生效）")
    private Integer calendarCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketItem implements Serializable {
        @ApiModelProperty("票种ID")
        private Long id;
        @ApiModelProperty("票种名称")
        private String name;
        @ApiModelProperty("单价（元）")
        private BigDecimal price;
        @ApiModelProperty("总库存")
        private Integer totalStock;
        @ApiModelProperty("已售数量")
        private Integer soldCount;
        @ApiModelProperty("每人限购")
        private Integer limitPerUser;
        @ApiModelProperty("状态：0-在售 1-停售")
        private Integer status;
    }
}
