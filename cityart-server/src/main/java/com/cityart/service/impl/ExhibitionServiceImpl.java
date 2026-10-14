package com.cityart.service.impl;

import com.cityart.constant.FavoriteTargetTypeConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.context.BaseContext;
import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.dto.ExhibitionWorkPageQueryDTO;
import com.cityart.enumeration.ExhibitionTypeEnum;
import com.cityart.exception.BaseException;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.ExhibitionWorkMapper;
import com.cityart.mapper.FavoriteMapper;
import com.cityart.result.PageResult;
import com.cityart.service.ExhibitionService;
import com.cityart.utils.ExhibitionStatusUtil;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.ExhibitionPageVO;
import com.cityart.vo.ExhibitionWorkVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExhibitionServiceImpl implements ExhibitionService {

    @Autowired
    private ExhibitionMapper exhibitionMapper;

    @Autowired
    private ExhibitionWorkMapper exhibitionWorkMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Override
    public PageResult<ExhibitionPageVO> pageQuery(ExhibitionPageQueryDTO dto) {
        // 1. 空字符串当作「不筛选」，避免 city='' 查不到任何数据
        dto.setCity(trimToNull(dto.getCity()));
        dto.setKeyword(trimToNull(dto.getKeyword()));

        // 2. 先查总条数（条件和列表查询共用同一段 SQL 片段，保证对得上）
        long total = exhibitionMapper.countExhibitionPage(dto);

        // 3. 再查当前页数据；总条数为 0 就不用再查一次数据库了
        List<ExhibitionPageVO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            list = exhibitionMapper.listExhibitionPage(dto);
            list.forEach(this::fillStatusAndTypeName);
        }

        // 4. pages（总页数）由 PageResult 统一算
        return PageResult.of(total, dto.getPage(), dto.getPageSize(), list);
    }

    @Override
    public ExhibitionDetailVO getDetailById(Long id) {
        // 1. 单条查询（SQL 里已经 JOIN 了 gallery，一次拿全，不用再查美术馆）
        ExhibitionDetailVO vo = exhibitionMapper.getExhibitionDetailById(id);
        if (vo == null) {
            // 全局异常处理器会把它转成 {"code":0,"msg":"展览不存在"}
            throw new BaseException(MessageConstant.EXHIBITION_NOT_FOUND);
        }

        // 2. 库里是逗号分隔字符串，拆成数组返回（详情页「展览实景」区块）
        vo.setImages(splitImages(vo.getImagesText()));

        // 3. 三个不落库的字段：展出状态实时算、类型中文名、剩余库存
        vo.setStatus(ExhibitionStatusUtil.of(vo.getStartDate(), vo.getEndDate()));
        vo.setTypeName(ExhibitionTypeEnum.labelOf(vo.getType()));
        int totalStock = vo.getTotalStock() == null ? 0 : vo.getTotalStock();
        int soldCount = vo.getSoldCount() == null ? 0 : vo.getSoldCount();
        vo.setRemainingStock(Math.max(totalStock - soldCount, 0));
        if (vo.getPrice() == null) {
            vo.setPrice(BigDecimal.ZERO);
        }

        // 4. 两个「非表字段」：
        //    · galleryPhone —— gallery 表暂时没有电话字段，恒为 null
        //    · isFavorited  —— 详情接口是「可选登录」：带 token 查真实收藏状态，游客为 false
        vo.setGalleryPhone(null);
        Long userId = BaseContext.getCurrentId();
        vo.setIsFavorited(userId != null
                && favoriteMapper.countByUserAndTarget(userId, FavoriteTargetTypeConstant.EXHIBITION, id) > 0);

        // 5. 作品集总数：前端用它决定「作品集」区块要不要显示（>0 才去调作品集接口）
        vo.setWorksCount((int) exhibitionWorkMapper.countByExhibitionId(id));
        return vo;
    }

    @Override
    public PageResult<ExhibitionWorkVO> pageWorks(Long exhibitionId, ExhibitionWorkPageQueryDTO dto) {
        // 作品集独立分页：即使展览不存在也只是查不到作品，返回空列表即可
        long total = exhibitionWorkMapper.countByExhibitionId(exhibitionId);

        List<ExhibitionWorkVO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            list = exhibitionWorkMapper.listByExhibitionId(exhibitionId, dto.getOffset(), dto.getLimit());
        }

        return PageResult.of(total, dto.getPage(), dto.getPageSize(), list);
    }

    /**
     * 补齐两个「不落库」的字段：展出状态、类型中文名；顺便把 null 票价按免费处理
     */
    private void fillStatusAndTypeName(ExhibitionPageVO vo) {
        vo.setStatus(ExhibitionStatusUtil.of(vo.getStartDate(), vo.getEndDate()));
        vo.setTypeName(ExhibitionTypeEnum.labelOf(vo.getType()));
        if (vo.getPrice() == null) {
            vo.setPrice(BigDecimal.ZERO);
        }
    }

    /**
     * 把「/a.jpg,/b.jpg」拆成数组；null、空串、多余空格都能正确处理
     */
    private List<String> splitImages(String imagesText) {
        if (imagesText == null || imagesText.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(imagesText.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    /** null / 空字符串 / 全空格 都返回 null */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
