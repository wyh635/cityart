package com.cityart.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 首页展览卡片返回数据
 * 「当前热门展览」和「艺览智荐」共用，推荐理由只有推荐接口才有，为空时不出现在 JSON 里
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("首页展览VO")
public class ExhibitionVO implements Serializable {

    @ApiModelProperty("展览ID")
    private Long id;

    @ApiModelProperty("展览标题")
    private String title;

    @ApiModelProperty("封面海报")
    private String posterImage;

    @ApiModelProperty("开展日期")
    private LocalDate startDate;

    @ApiModelProperty("闭展日期")
    private LocalDate endDate;

    @ApiModelProperty("票价（元），0 表示免费")
    private BigDecimal price;

    @ApiModelProperty("所属美术馆ID")
    private Long galleryId;

    @ApiModelProperty("美术馆名称（JOIN gallery 取得）")
    private String galleryName;

    @ApiModelProperty("展出状态：current 正在展出 / future 即将展出 / past 往期展出")
    private String status;

    @ApiModelProperty("推荐理由（算法侧返回，仅「艺览智荐」有值）")
    private String recommendReason;
}
