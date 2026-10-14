package com.cityart.service;

import com.cityart.dto.FavoriteDTO;
import com.cityart.dto.FavoritePageQueryDTO;
import com.cityart.vo.FavoritePageVO;

/**
 * 收藏业务
 */
public interface FavoriteService {

    /** 收藏列表分页（targetType=1 展览 / 2 美术馆） */
    FavoritePageVO<Object> pageFavorites(Long userId, FavoritePageQueryDTO dto);

    /** 添加收藏（幂等，重复收藏不报错） */
    void add(Long userId, FavoriteDTO dto);

    /** 取消收藏 */
    void remove(Long userId, FavoriteDTO dto);

    /** 是否已收藏 */
    boolean check(Long userId, Integer targetType, Long targetId);
}
