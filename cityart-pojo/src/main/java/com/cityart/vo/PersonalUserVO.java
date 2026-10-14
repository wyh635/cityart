package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 个人中心的用户信息（手机号是脱敏后的）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("个人中心用户VO")
public class PersonalUserVO implements Serializable {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("脱敏手机号，如 138****1221（不下发完整号码）")
    private String phone;

    @ApiModelProperty("所在城市")
    private String city;

    @ApiModelProperty("0-未知 1-男 2-女")
    private Integer gender;
}
