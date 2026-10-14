package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 个人中心的数据统计三块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("个人中心统计VO")
public class UserStatisticsVO implements Serializable {

    @ApiModelProperty("我的收藏数")
    private Integer favoriteCount;

    @ApiModelProperty("有效票夹数（已支付订单数）")
    private Integer validTicketCount;

    @ApiModelProperty("观展次数（读 user.visit_count 冗余字段）")
    private Integer visitCount;
}
