package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * C端用户登录
 */
@Data
@ApiModel("用户注册/登录DTO")
public class UserLoginDTO implements Serializable {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    @ApiModelProperty("手机号")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度6‑16位")
    @ApiModelProperty("密码")
    private String password;

}
