package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * B 端管理员修改个人资料入参
 * <p>
 * 对应原型「编辑个人资料」：昵称（姓名）/ 头像 / 所在城市。
 * **手机号不允许修改**，所以这里没有 phone 字段（前端传了也会被忽略）。
 */
@Data
@ApiModel("管理员个人资料DTO")
public class AdminProfileDTO implements Serializable {

    @Size(max = 50, message = "姓名最长 50 个字符")
    @ApiModelProperty("管理员姓名")
    private String name;

    @ApiModelProperty("头像URL（先调上传接口拿到）")
    private String avatar;

    @Size(max = 50, message = "城市名最长 50 个字符")
    @ApiModelProperty("所在城市，如：南京")
    private String city;

}
