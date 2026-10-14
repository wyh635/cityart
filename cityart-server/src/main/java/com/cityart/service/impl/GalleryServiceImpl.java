package com.cityart.service.impl;

import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.enumeration.GalleryTypeEnum;
import com.cityart.mapper.GalleryMapper;
import com.cityart.result.PageResult;
import com.cityart.service.GalleryService;
import com.cityart.vo.GalleryPageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GalleryServiceImpl implements GalleryService {

    @Autowired
    private GalleryMapper galleryMapper;

    @Override
    public PageResult<GalleryPageVO> pageQuery(GalleryPageQueryDTO dto) {
        // 1. 空字符串当作「不筛选」
        dto.setCity(trimToNull(dto.getCity()));
        dto.setKeyword(trimToNull(dto.getKeyword()));

        // 2. 总条数（页面标题「共有 N 个美术馆」也用它）
        long total = galleryMapper.countGalleryPage(dto);

        // 3. 当前页数据
        List<GalleryPageVO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            list = galleryMapper.listGalleryPage(dto);
            // 类型中文名由后端维护，前端不硬编码
            list.forEach(vo -> vo.setTypeName(GalleryTypeEnum.labelOf(vo.getType())));
        }

        return PageResult.of(total, dto.getPage(), dto.getPageSize(), list);
    }

    /** null / 空字符串 / 全空格 都返回 null */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
