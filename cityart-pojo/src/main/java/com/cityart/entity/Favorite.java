package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏，对应数据库表 favorite
 * <p>
 * target_type：1-展览 2-美术馆；同一个用户对同一个对象只能收藏一次（uk_user_target）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Favorite implements Serializable {

    private Long id;

    private Long userId;

    /** 1-收藏的展览 2-收藏的美术馆 */
    private Integer targetType;

    /** 收藏对象ID（展览ID 或 美术馆ID） */
    private Long targetId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
