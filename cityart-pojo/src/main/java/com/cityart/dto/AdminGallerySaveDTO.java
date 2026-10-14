package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 后台「新增 / 编辑展馆」入参
 * <p>
 * 对应原型「新增展馆」页：基础信息 + 展馆海报 + 展馆简历。
 * <p>
 * ⚠️ 关于原型上「闭馆日期 *」这个字段名：
 * 原型文案有歧义（日期 vs 时间）。本设计按**闭馆时间**理解，落到 {@code closeHour}；
 * 另设 {@code closedDay} 表示「固定闭馆日」（如周一）。
 */
@Data
@ApiModel("后台展馆保存DTO")
public class AdminGallerySaveDTO implements Serializable {

    @NotBlank(message = "展馆名称不能为空")
    @Size(max = 100, message = "展馆名称最长 100 字")
    @ApiModelProperty("展馆名称 *")
    private String name;

    @NotBlank(message = "请选择所属城市")
    @Size(max = 50, message = "城市名最长 50 字")
    @ApiModelProperty("所属城市 *（如：南京）")
    private String city;

    @NotBlank(message = "请填写详细地址")
    @Size(max = 255, message = "地址最长 255 字")
    @ApiModelProperty("详细地址 *（已含城市与区县）")
    private String address;

    @NotBlank(message = "请填写开馆时间")
    @Size(max = 20, message = "开馆时间格式过长")
    @ApiModelProperty("开馆时间 *（如 08:30）")
    private String openHour;

    @NotBlank(message = "请填写闭馆时间")
    @Size(max = 20, message = "闭馆时间格式过长")
    @ApiModelProperty("闭馆时间 *（原型「闭馆日期」，按时间理解，如 18:30）")
    private String closeHour;

    @Size(max = 50, message = "固定闭馆日最长 50 字")
    @ApiModelProperty("固定闭馆日（如：周一），用于自动拼 C 端展示的 openTime")
    private String closedDay;

    @NotBlank(message = "请上传展馆海报")
    @Size(max = 255, message = "图片地址过长")
    @ApiModelProperty("展馆海报 *（先调上传接口拿到 URL）")
    private String coverImage;

    @NotBlank(message = "请填写展馆简历")
    @ApiModelProperty("展馆简介 / 展馆简历长文 *")
    private String intro;

    @Min(value = 1, message = "展馆类型只能是 1-5")
    @Max(value = 5, message = "展馆类型只能是 1-5")
    @ApiModelProperty("展馆类型：1-综合 2-当代 3-古典 4-雕塑 5-摄影，默认 1")
    private Integer type = 1;

    @Min(value = 0, message = "isHot 只能是 0 或 1")
    @Max(value = 1, message = "isHot 只能是 0 或 1")
    @ApiModelProperty("是否上首页热门：0-否 1-是")
    private Integer isHot = 0;

    @Min(value = 0, message = "状态只能是 0-正常营业 1-闭馆维护")
    @Max(value = 1, message = "状态只能是 0-正常营业 1-闭馆维护")
    @ApiModelProperty("状态：0-正常营业 1-闭馆维护")
    private Integer status = 0;
}
