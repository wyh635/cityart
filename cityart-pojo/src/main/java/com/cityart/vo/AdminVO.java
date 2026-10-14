package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * B 端管理员登录返回
 * <p>
 * 只返回登录后首次渲染需要的字段，**不含 password**。
 * 页面刷新时前端只用 token 调 /api/admin/profile 重新拉取。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVO implements Serializable {

    @ApiModelProperty("管理员ID")
    private Long id;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("管理员姓名")
    private String name;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("所在城市")
    private String city;

    @ApiModelProperty("JWT令牌（B 端密钥签发）")
    private String token;

}
