package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 后台「展览管理 - 展览列表」一行
 * <p>
 * 对应原型表格列：展览ID / 展览名称 / 所属场馆 / 展期 / 票价 / 当日库存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExhibitionVO implements Serializable {

    @ApiModelProperty("展览ID（自增主键）")
    private Long id;

    @ApiModelProperty("展览编号，如 NJ001（原型「展览ID」列显示的其实是这个）")
    private String exhibitionCode;

    @ApiModelProperty("展览名称")
    private String title;

    @ApiModelProperty("所属场馆ID")
    private Long galleryId;

    @ApiModelProperty("所属场馆名称")
    private String galleryName;

    @ApiModelProperty("所在城市（筛选用，不展示）")
    private String city;

    @ApiModelProperty("开展日期")
    private LocalDate startDate;

    @ApiModelProperty("闭展日期")
    private LocalDate endDate;

    @ApiModelProperty("展览分类：1-综合 2-当代 3-古典 4-雕塑 5-摄影")
    private Integer type;

    @ApiModelProperty("票价（元）")
    private BigDecimal price;

    @ApiModelProperty("每日票数上限（运营在新增展览时填的）")
    private Integer dailyStockLimit;

    @ApiModelProperty("上下架状态：0-上架 1-下架")
    private Integer status;

    @ApiModelProperty("是否热门：0-否 1-是")
    private Integer isHot;

    // ===== 下面三个由 Service 计算后回填，不是数据库列 =====

    @ApiModelProperty("当日（最近放票日）剩余库存")
    private Integer dailyStock;

    @ApiModelProperty("库存状态：available-有票 soldOut-已售完")
    private String stockStatus;

    @ApiModelProperty("库存文案：原型显示「42张」或「已售完」")
    private String stockStatusText;

}
