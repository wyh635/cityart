package com.cityart.service;

import com.cityart.vo.BannerVO;
import com.cityart.vo.ExhibitionVO;
import com.cityart.vo.GalleryVO;

import java.util.List;

/**
 * 首页模块业务接口（轮播图 + 当前热门展览 + 艺览智荐 + 热门美术馆）
 */
public interface HomeService {

    /** 首页顶部轮播图 */
    List<BannerVO> listBanners();

    /** 首页「当前热门展览」：is_hot = 1 且今天在展期内 */
    List<ExhibitionVO> listHotExhibitions(String city, Integer limit);

    /** 首页「艺览智荐」：目前用「热门且在展期内」的展览兜底 */
    List<ExhibitionVO> listRecommendExhibitions(Long userId, Integer limit);

    /** 首页「热门美术馆」：is_hot = 1 且 status = 0 */
    List<GalleryVO> listHotGalleries(String city, Integer limit);
}
