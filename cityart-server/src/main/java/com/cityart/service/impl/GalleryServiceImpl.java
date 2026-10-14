package com.cityart.service.impl;

import com.cityart.constant.FavoriteTargetTypeConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.context.BaseContext;
import com.cityart.dto.GalleryExhibitionPageQueryDTO;
import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.enumeration.ExhibitionTypeEnum;
import com.cityart.enumeration.GalleryTypeEnum;
import com.cityart.exception.BaseException;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.FavoriteMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.result.PageResult;
import com.cityart.service.GalleryService;
import com.cityart.utils.ExhibitionStatusUtil;
import com.cityart.vo.GalleryDetailVO;
import com.cityart.vo.GalleryExhibitionCountVO;
import com.cityart.vo.GalleryExhibitionVO;
import com.cityart.vo.GalleryPageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GalleryServiceImpl implements GalleryService {

    @Autowired
    private GalleryMapper galleryMapper;

    @Autowired
    private ExhibitionMapper exhibitionMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

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

    @Override
    public GalleryDetailVO getDetailById(Long id) {
        // 1. 主体信息（SQL 带 status = 0，闭馆的馆查不到）
        GalleryDetailVO vo = galleryMapper.getGalleryById(id);
        if (vo == null) {
            // 全局异常处理器会把它转成 {"code":0,"msg":"美术馆不存在"}
            throw new BaseException(MessageConstant.GALLERY_NOT_FOUND);
        }

        // 2. 类型中文名
        vo.setTypeName(GalleryTypeEnum.labelOf(vo.getType()));

        // 3. 收藏状态：详情接口是「可选登录」——
        //    带 token 时 BaseContext 里有 userId，查真实收藏状态；游客为 false
        Long userId = BaseContext.getCurrentId();
        boolean favorited = userId != null
                && favoriteMapper.countByUserAndTarget(userId, FavoriteTargetTypeConstant.GALLERY, id) > 0;
        vo.setIsFavorited(favorited);

        // 4. 两个分组的展览数量（前端据此渲染分组标题、决定要不要调展览列表接口）
        GalleryExhibitionCountVO count = exhibitionMapper.getGalleryExhibitionCount(id);
        vo.setCurrentCount(count == null || count.getCurrentCount() == null ? 0 : count.getCurrentCount());
        vo.setPastCount(count == null || count.getPastCount() == null ? 0 : count.getPastCount());
        return vo;
    }

    @Override
    public PageResult<GalleryExhibitionVO> pageExhibitions(Long galleryId, GalleryExhibitionPageQueryDTO dto) {
        // 1. 总条数（条件与列表查询共用同一段 SQL 片段）
        long total = exhibitionMapper.countGalleryExhibitions(galleryId, dto);

        // 2. 当前页数据
        List<GalleryExhibitionVO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            list = exhibitionMapper.listGalleryExhibitions(galleryId, dto);
            list.forEach(this::fillStatusAndTypeName);
        }

        return PageResult.of(total, dto.getPage(), dto.getPageSize(), list);
    }

    /**
     * 补齐两个「不落库」的字段：展出状态、类型中文名；null 票价按免费处理
     */
    private void fillStatusAndTypeName(GalleryExhibitionVO vo) {
        vo.setStatus(ExhibitionStatusUtil.of(vo.getStartDate(), vo.getEndDate()));
        vo.setTypeName(ExhibitionTypeEnum.labelOf(vo.getType()));
        if (vo.getPrice() == null) {
            vo.setPrice(BigDecimal.ZERO);
        }
    }

    /** null / 空字符串 / 全空格 都返回 null */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
