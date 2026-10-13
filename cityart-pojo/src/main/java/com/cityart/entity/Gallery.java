package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 美术馆，对应数据库表 gallery
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gallery implements Serializable {

    private Long id;

    /** 美术馆名称 */
    private String name;

    /** 封面图（唯一图片，首页卡片与详情页共用） */
    private String coverImage;

    /** 详细地址（已含城市与区县） */
    private String address;

    /** 所在城市，用于筛选 */
    private String city;

    /** 展馆简介长文 */
    private String intro;

    /** 开放时间，如：周二到周日 8:30-18:30 */
    private String openTime;

    /** 当前在展数量（冗余字段） */
    private Integer exhibitionCount;

    /** 是否热门：0-否 1-是 */
    private Integer isHot;

    /** 状态：0-正常营业 1-闭馆维护 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
