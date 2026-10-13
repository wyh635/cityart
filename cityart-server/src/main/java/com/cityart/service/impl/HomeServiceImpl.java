package com.cityart.service.impl;

import com.cityart.mapper.BannerMapper;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.HomeService;
import com.cityart.utils.ExhibitionStatusUtil;
import com.cityart.vo.BannerVO;
import com.cityart.vo.ExhibitionVO;
import com.cityart.vo.GalleryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class HomeServiceImpl implements HomeService {

    /** limit 兜底值：Controller 已经校验过，这里再保护一次，防止被别人直接调用时传 null */
    private static final int DEFAULT_EXHIBITION_LIMIT = 10;
    private static final int DEFAULT_GALLERY_LIMIT = 4;
    private static final int DEFAULT_RECOMMEND_LIMIT = 4;
    private static final int MAX_LIMIT = 20;

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private ExhibitionMapper exhibitionMapper;

    @Autowired
    private GalleryMapper galleryMapper;

    @Override
    public List<BannerVO> listBanners() {
        return bannerMapper.listBanners();
    }

    @Override
    public List<ExhibitionVO> listHotExhibitions(String city, Integer limit) {
        List<ExhibitionVO> list = exhibitionMapper.listHotExhibitions(
                normalizeCity(city), normalizeLimit(limit, DEFAULT_EXHIBITION_LIMIT));
        // 展出状态不落库，查出来之后按今天实时计算
        list.forEach(this::fillStatusAndPrice);
        return list;
    }

    @Override
    public List<ExhibitionVO> listRecommendExhibitions(Long userId, Integer limit) {
        // TODO 算法团队接口就绪后，这里改成：把 userId + limit 转发给算法服务，拿到展览 ID 列表后再回查图文信息。
        //      当前先用「热门且在展期内」的展览兜底（未登录用户也能看到内容）。
        log.info("艺览智荐（兜底实现），userId：{}", userId);
        int size = normalizeLimit(limit, DEFAULT_RECOMMEND_LIMIT);
        List<ExhibitionVO> list = exhibitionMapper.listHotExhibitions(null, size);
        list.forEach(vo -> {
            fillStatusAndPrice(vo);
            vo.setRecommendReason("热门推荐");
        });
        return list;
    }

    @Override
    public List<GalleryVO> listHotGalleries(String city, Integer limit) {
        return galleryMapper.listHotGalleries(
                normalizeCity(city), normalizeLimit(limit, DEFAULT_GALLERY_LIMIT));
    }

    /**
     * 计算展出状态：正在展出 / 即将展出 / 往期展出
     */
    private void fillStatusAndPrice(ExhibitionVO vo) {
        // 票价为 null 时按 0（免费）返回，前端不用再判空
        if (vo.getPrice() == null) {
            vo.setPrice(BigDecimal.ZERO);
        }
        // 展出状态统一由工具类按今天实时计算（列表页用的是同一个工具类，规则只维护一处）
        vo.setStatus(ExhibitionStatusUtil.of(vo.getStartDate(), vo.getEndDate()));
    }

    /** 城市参数：null 或空字符串都当作「不筛选」 */
    private String normalizeCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return null;
        }
        return city.trim();
    }

    /** 条数参数：为空或非法时用默认值，且不允许超过 20 */
    private int normalizeLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit < 1) {
            return defaultLimit;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
