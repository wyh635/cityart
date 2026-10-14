package com.cityart.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * B 端管理员个人信息（后台「个人中心」页）
 * <p>
 * 对应原型：头像 / 昵称（姓名）/ 城市 / 编辑个人资料 / 退出登录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileVO implements Serializable {

    @ApiModelProperty("管理员ID")
    private Long id;

    @ApiModelProperty("管理员姓名（原型显示「林一」）")
    private String name;

    @ApiModelProperty("手机号，脱敏返回，如 138****0000")
    private String phone;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("所在城市（原型昵称下方的「南京」）")
    private String city;

    @ApiModelProperty("管理员编号")
    private String adminCode;

    @ApiModelProperty("角色名称（当前表无角色字段，预留）")
    private String role;

    @ApiModelProperty("账号状态：0-启用 1-禁用")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

}
