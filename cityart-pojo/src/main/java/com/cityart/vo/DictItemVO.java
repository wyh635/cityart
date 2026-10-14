package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字典项（下拉框的一个选项），三个字典接口共用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("字典项VO")
public class DictItemVO implements Serializable {

    @ApiModelProperty("传给列表接口的参数值")
    private Integer value;

    @ApiModelProperty("下拉框显示的中文名")
    private String label;
}
