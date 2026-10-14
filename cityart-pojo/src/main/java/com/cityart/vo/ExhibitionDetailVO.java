package com.cityart.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
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
 * 展览详情页主体数据（头部信息 + 展览简介 + 展览实景 + 票务信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("展览详情VO")
public class ExhibitionDetailVO implements Serializable {

    @ApiModelProperty("展览ID")
    private Long id;

    @ApiModelProperty("展览标题")
    private String title;

    @ApiModelProperty("封面海报（顶部大图）")
    private String posterImage;

    @ApiModelProperty("开展日期")
    private LocalDate startDate;

    @ApiModelProperty("闭展日期")
    private LocalDate endDate;

    @ApiModelProperty("展览开放时间")
    private String openTime;

    @ApiModelProperty("展出状态：current / future / past（Service 实时计算）")
    private String status;

    @ApiModelProperty("展览分类值：1-5")
    private Integer type;

    @ApiModelProperty("展览分类中文名")
    private String typeName;

    @ApiModelProperty("展览简介长文")
    private String description;

    /**
     * 数据库里 images 是「英文逗号分隔的字符串」，直接返回给前端没法用，
     * 所以用 @JsonIgnore 把它挡在 JSON 之外，Service 拆好数组后填到下面的 images 字段
     */
    @JsonIgnore
    private String imagesText;

    @ApiModelProperty("展览实景图数组（由 imagesText 拆出来）")
    private List<String> images;

    @ApiModelProperty("所属美术馆ID")
    private Long galleryId;

    @ApiModelProperty("所属美术馆名称")
    private String galleryName;

    @ApiModelProperty("美术馆地址")
    private String galleryAddress;

    @ApiModelProperty("美术馆电话（gallery 表暂无 phone 字段，恒为 null，保留字段位以便后续扩展）")
    private String galleryPhone;

    @ApiModelProperty("票价（元），0 表示免费")
    private BigDecimal price;

    @ApiModelProperty("总库存（总票数）")
    private Integer totalStock;

    @ApiModelProperty("已售数量")
    private Integer soldCount;

    @ApiModelProperty("剩余库存 = totalStock - soldCount（Service 计算，不落库）")
    private Integer remainingStock;

    @ApiModelProperty("当前用户是否已收藏；未登录（游客）恒为 false")
    private Boolean isFavorited;

    @ApiModelProperty("作品集总数，>0 时前端才去调作品集接口")
    private Integer worksCount;
}
