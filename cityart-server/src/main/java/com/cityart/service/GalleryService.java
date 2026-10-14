package com.cityart.service;

import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.GalleryPageVO;

/**
 * 「美术馆」列表业务
 */
public interface GalleryService {

    /**
     * 分页查询美术馆列表（只查正常营业的馆）
     */
    PageResult<GalleryPageVO> pageQuery(GalleryPageQueryDTO dto);
}
