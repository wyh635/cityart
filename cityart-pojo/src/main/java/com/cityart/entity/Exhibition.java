package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 展览，对应数据库表 exhibition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exhibition implements Serializable {

    private Long id;

    /** 所属美术馆ID，关联 gallery.id */
    private Long galleryId;

    /** 展览标题 */
    private String title;

    /** 封面海报图 */
    private String posterImage;

    /** 展览实景图，多张用英文逗号分隔 */
    private String images;

    /** 开展日期 */
    private LocalDate startDate;

    /** 闭展日期 */
    private LocalDate endDate;

    /** 展览开放时间 */
    private String openTime;

    /** 展览简介长文 */
    private String description;

    /** 是否热门：0-否 1-是 */
    private Integer isHot;

    /** 排序权重，数字越大越靠前 */
    private Integer sortOrder;

    /** 展览分类：1-当代 2-古典 3-雕塑 4-摄影 */
    private Integer type;

    /** 票价（单一定价，元） */
    private BigDecimal price;

    /** 总库存（票数上限） */
    private Integer totalStock;

    /** 已售数量 */
    private Integer soldCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
