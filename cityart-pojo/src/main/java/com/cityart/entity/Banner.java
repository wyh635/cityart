package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页轮播图，对应数据库表 banner
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Banner implements Serializable {

    private Long id;

    /** 轮播大图地址（文案已烧入图中） */
    private String imageUrl;

    /** 标题，仅后台识别使用，页面不展示 */
    private String title;

    /** 排序，数字越大越靠前 */
    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
