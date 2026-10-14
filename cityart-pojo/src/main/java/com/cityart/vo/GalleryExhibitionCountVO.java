package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 某个美术馆下「正在展出 / 往期展出」的展览数量
 * （美术馆详情页两个分组标题右侧的数字）
 */
@Data
@ApiModel("馆内展览数量VO")
public class GalleryExhibitionCountVO implements Serializable {

    @ApiModelProperty("正在展出数量")
    private Integer currentCount;

    @ApiModelProperty("往期展出数量")
    private Integer pastCount;
}
