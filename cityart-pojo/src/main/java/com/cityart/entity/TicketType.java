package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票种，对应数据库表 ticket_type
 * <p>
 * 一个展览多个票种多个价格，价格与库存都以本表为准
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketType implements Serializable {

    private Long id;

    /** 所属展览ID */
    private Long exhibitionId;

    /** 票种名称：成人票 / 老人票 / 学生票 / 儿童票 */
    private String name;

    /** 该票种单价 */
    private BigDecimal price;

    /** 总库存 */
    private Integer totalStock;

    /** 已售数量 */
    private Integer soldCount;

    /** 每人限购数量 */
    private Integer limitPerUser;

    /** 排序，数字越大越靠前 */
    private Integer sort;

    /** 状态：0-在售 1-停售 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
