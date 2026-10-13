package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("JWT令牌")
    private String token;

}
