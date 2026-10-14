package com.cityart.service.impl;

import com.cityart.constant.FavoriteTargetTypeConstant;
import com.cityart.dto.FavoriteDTO;
import com.cityart.dto.FavoritePageQueryDTO;
import com.cityart.mapper.FavoriteMapper;
import com.cityart.service.FavoriteService;
import com.cityart.vo.FavoriteExhibitionVO;
import com.cityart.vo.FavoriteGalleryVO;
import com.cityart.vo.FavoritePageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Override
    public FavoritePageVO<Object> pageFavorites(Long userId, FavoritePageQueryDTO dto) {
        // 两个 tab 的徽标数字都要返回，前端才能显示「展览 4 / 美术馆 2」
        int exhibitionCount = favoriteMapper.countByUserAndType(userId, FavoriteTargetTypeConstant.EXHIBITION);
        int galleryCount = favoriteMapper.countByUserAndType(userId, FavoriteTargetTypeConstant.GALLERY);

        int targetType = dto.getTargetType() == null ? FavoriteTargetTypeConstant.EXHIBITION : dto.getTargetType();
        long total;
        List<Object> items;

        if (FavoriteTargetTypeConstant.GALLERY.equals(targetType)) {
            total = galleryCount;
            List<FavoriteGalleryVO> list = total == 0
                    ? Collections.emptyList()
                    : favoriteMapper.listFavoriteGalleries(userId, dto.getOffset(), dto.getLimit());
            items = new ArrayList<>(list);
        } else {
            total = exhibitionCount;
            List<FavoriteExhibitionVO> list = total == 0
                    ? Collections.emptyList()
                    : favoriteMapper.listFavoriteExhibitions(userId, dto.getOffset(), dto.getLimit());
            items = new ArrayList<>(list);
        }

        return FavoritePageVO.of(total, dto.getPage(), dto.getLimit(), items, exhibitionCount, galleryCount);
    }

    @Override
    public void add(Long userId, FavoriteDTO dto) {
        // INSERT IGNORE + uk_user_target：重复收藏影响行数为 0，也算成功（幂等）
        int rows = favoriteMapper.insertIgnore(userId, dto.getTargetType(), dto.getTargetId());
        log.info("添加收藏，userId：{}，targetType：{}，targetId：{}，影响行数：{}",
                userId, dto.getTargetType(), dto.getTargetId(), rows);
    }

    @Override
    public void remove(Long userId, FavoriteDTO dto) {
        favoriteMapper.delete(userId, dto.getTargetType(), dto.getTargetId());
    }

    @Override
    public boolean check(Long userId, Integer targetType, Long targetId) {
        return favoriteMapper.countByUserAndTarget(userId, targetType, targetId) > 0;
    }
}
