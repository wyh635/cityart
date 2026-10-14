package com.cityart.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付参数（支付页「去支付」）
 * <p>
 * ⚠️ JSON 里的 `package` 是 Java 关键字，不能当字段名，
 * 所以字段叫 packageValue，用 @JsonProperty("package") 指定输出成 package。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("支付参数VO")
public class PayParamVO implements Serializable {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("支付方式：1-微信 2-支付宝")
    private Integer payType;

    @ApiModelProperty("应付金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("扫码支付二维码内容")
    private String qrCodeUrl;

    @ApiModelProperty("预支付交易会话标识")
    private String prepayId;

    @ApiModelProperty("时间戳")
    private String timeStamp;

    @ApiModelProperty("随机字符串")
    private String nonceStr;

    @JsonProperty("package")
    @ApiModelProperty("订单详情扩展字符串（小程序调起支付用）")
    private String packageValue;

    @ApiModelProperty("签名方式")
    private String signType;

    @ApiModelProperty("签名")
    private String paySign;
}
