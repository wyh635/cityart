package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * B 端管理员登录入参
 * <p>
 * 管理员没有注册入口（账号由已有管理员在后台手动新增），所以只有登录，没有注册 DTO。
 */
@Data
@ApiModel("管理员登录DTO")
public class AdminLoginDTO implements Serializable {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    @ApiModelProperty("手机号（登录账号）")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度6-16位")
    @ApiModelProperty("密码")
    private String password;

}
