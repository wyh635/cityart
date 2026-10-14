package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 编辑个人资料
 * <p>
 * ⚠️ 手机号**不可修改**（原型上是只读展示），所以本 DTO 里**故意没有 phone 字段**，
 * 即使前端多传了 phone，Jackson 也会忽略（FAIL_ON_UNKNOWN_PROPERTIES = false）。
 */
@Data
@ApiModel("编辑个人资料DTO")
public class ProfileUpdateDTO implements Serializable {

    @Size(min = 2, max = 20, message = "昵称长度 2~20 个字符")
    @ApiModelProperty("昵称")
    private String nickname;

    @Min(value = 0, message = "gender 只能是 0-未知 1-男 2-女")
    @Max(value = 2, message = "gender 只能是 0-未知 1-男 2-女")
    @ApiModelProperty("0-未知 1-男 2-女")
    private Integer gender;

    @Size(max = 50, message = "城市名称最多 50 个字符")
    @ApiModelProperty("所在城市")
    private String city;
}
