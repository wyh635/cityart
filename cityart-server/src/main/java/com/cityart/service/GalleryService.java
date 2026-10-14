package com.cityart.service;

import com.cityart.dto.GalleryExhibitionPageQueryDTO;
import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.GalleryDetailVO;
import com.cityart.vo.GalleryExhibitionVO;
import com.cityart.vo.GalleryPageVO;

/**
 * 美术馆业务（美术馆列表 + 美术馆详情 + 馆内展览列表）
 */
public interface GalleryService {

    /**
     * 分页查询美术馆列表（只查正常营业的馆）
     */
    PageResult<GalleryPageVO> pageQuery(GalleryPageQueryDTO dto);

    /**
     * 美术馆详情（详情页主体数据 + 两个分组的展览数量）
     *
     * @param id 美术馆 ID，查不到抛「美术馆不存在」
     */
    GalleryDetailVO getDetailById(Long id);

    /**
     * 美术馆详情页的展览列表（正在展出 / 往期展出共用，靠 status 区分）
     */
    PageResult<GalleryExhibitionVO> pageExhibitions(Long galleryId, GalleryExhibitionPageQueryDTO dto);
}
