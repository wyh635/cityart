package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 个人中心聚合信息（顶部个人信息 + 数据统计）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("个人中心VO")
public class PersonalCenterVO implements Serializable {

    @ApiModelProperty("顶部个人信息")
    private PersonalUserVO user;

    @ApiModelProperty("数据统计")
    private UserStatisticsVO statistics;
}
