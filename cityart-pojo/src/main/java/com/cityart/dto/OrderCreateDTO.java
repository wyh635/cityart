package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 创建订单（购票页「确认下单」）
 */
@Data
@ApiModel("创建订单DTO")
public class OrderCreateDTO implements Serializable {

    @NotNull(message = "展览ID不能为空")
    @ApiModelProperty("展览ID")
    private Long exhibitionId;

    @NotNull(message = "票种ID不能为空")
    @ApiModelProperty("票种ID（来自票种列表接口）")
    private Long ticketTypeId;

    @NotNull(message = "观展日期不能为空")
    @ApiModelProperty("观展日期 yyyy-MM-dd，必须在日历接口返回的列表里")
    private LocalDate visitDate;

    @NotNull(message = "购票数量不能为空")
    @Min(value = 1, message = "购票数量至少 1 张")
    @Max(value = 99, message = "购票数量超出范围")
    @ApiModelProperty("数量，≥1 且不超过该票种的每人限购数")
    private Integer quantity;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人最多 50 个字符")
    @ApiModelProperty("联系人")
    private String contactName;

    @NotBlank(message = "联系手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    @ApiModelProperty("联系手机号，11 位")
    private String contactPhone;
}
