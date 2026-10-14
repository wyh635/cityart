package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 展览订单，对应数据库表 exhibition_order
 * <p>
 * status：1-待支付 2-已支付 3-已取消 4-已退款 5-已完成（核销入场）
 * 一单一码：一个订单一个 qr_code，核销按整单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionOrder implements Serializable {

    private Long id;

    /** 订单号（不用自增 id，靠 uk_order_no 保唯一） */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    private Long exhibitionId;

    private Long galleryId;

    private Long ticketTypeId;

    /** 票种名称快照（票种改名后历史订单不受影响） */
    private String ticketName;

    /** 单价快照 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 订单总金额 = price × quantity */
    private BigDecimal totalAmount;

    /** 1-待支付 2-已支付 3-已取消 4-已退款 5-已完成 */
    private Integer status;

    /** 观展日期 */
    private LocalDate visitDate;

    /** 核销二维码（支付成功后生成） */
    private String qrCode;

    /** 核销时间（不为空说明已入场） */
    private LocalDateTime useTime;

    /** 票的有效期（支付成功后按观展日期 + 45 天生成） */
    private LocalDateTime expireTime;

    private String contactName;

    private String contactPhone;

    /** 支付方式：1-微信 2-支付宝 */
    private Integer payType;

    private LocalDateTime payTime;

    /** 支付超时时间（下单 + 30 分钟） */
    private LocalDateTime payExpireTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    /** 退款状态：0-未退款 1-申请中 2-已退款 */
    private Integer refundStatus;

    private BigDecimal refundAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
