package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 后台「新增展览」入参
 * <p>
 * 对应原型四块：基础信息 / 票务数据 / 每日票数上限 / 展览海报
 */
@Data
@ApiModel("后台新增展览DTO")
public class AdminExhibitionSaveDTO implements Serializable {

    // ==================== 基础信息 ====================

    @NotBlank(message = "展览名称不能为空")
    @Size(max = 200, message = "展览名称最长 200 字")
    @ApiModelProperty("展览名称 *")
    private String title;

    @NotNull(message = "请选择所属展馆")
    @ApiModelProperty("所属展馆ID *")
    private Long galleryId;

    @NotNull(message = "请选择开展日期")
    @ApiModelProperty("开展日期 *")
    private LocalDate startDate;

    @NotNull(message = "请选择闭展日期")
    @ApiModelProperty("闭展日期 *")
    private LocalDate endDate;

    @ApiModelProperty("展览分类：1-综合 2-当代 3-古典 4-雕塑 5-摄影")
    private Integer type = 1;

    @ApiModelProperty("展览开放时间，如 周二到周日 8:30-18:30")
    private String openTime;

    // ==================== 每日票数上限 ====================

    @NotNull(message = "请填写每日票数上限")
    @Min(value = 1, message = "每日票数上限最小为 1")
    @ApiModelProperty("每日票数上限 *（建展览时按展期批量生成日历库存）")
    private Integer dailyStockLimit;

    // ==================== 展览海报 ====================

    @NotBlank(message = "请上传展览海报")
    @ApiModelProperty("展览海报 *（先调上传接口拿到 URL）")
    private String posterImage;

    @ApiModelProperty("展览实景图，多张用英文逗号分隔")
    private String images;

    @ApiModelProperty("展览简介长文")
    private String description;

    @ApiModelProperty("是否上首页热门：0-否 1-是")
    private Integer isHot = 0;

    @ApiModelProperty("排序权重，数字越大越靠前")
    private Integer sortOrder = 0;

    // ==================== 票务数据 ====================

    /**
     * 原型上是四个独立输入框：成人票 / 学生票 / 老年票 / 儿童票。
     * 只传填了价格的票种，没填的就不建该票种。
     * <p>
     * totalStock 不传时默认用 dailyStockLimit × 展期天数，保证整场展览的票够卖。
     */
    @Valid
    @ApiModelProperty("票种列表（成人票必填，其余可空）")
    private List<TicketItem> tickets;

    @Data
    @ApiModel("票种项")
    public static class TicketItem implements Serializable {

        @NotBlank(message = "票种名称不能为空")
        @ApiModelProperty("票种名称：成人票 / 学生票 / 老年票 / 儿童票")
        private String name;

        @NotNull(message = "票价不能为空")
        @Min(value = 0, message = "票价不能为负")
        @ApiModelProperty("票种单价（元），0 表示免费")
        private BigDecimal price;

        @ApiModelProperty("总库存，不传则按 每日票数上限 × 展期天数 自动算")
        private Integer totalStock;

        @Min(value = 1, message = "限购至少 1 张")
        @Max(value = 99, message = "限购最多 99 张")
        @ApiModelProperty("每人限购数量，默认 5")
        private Integer limitPerUser = 5;

        @ApiModelProperty("排序，数字越大越靠前")
        private Integer sort = 0;
    }
}
