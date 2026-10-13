package com.cityart.service.impl;

import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.enumeration.ExhibitionTypeEnum;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.result.PageResult;
import com.cityart.service.ExhibitionService;
import com.cityart.utils.ExhibitionStatusUtil;
import com.cityart.vo.ExhibitionPageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ExhibitionServiceImpl implements ExhibitionService {

    @Autowired
    private ExhibitionMapper exhibitionMapper;

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

    /** null / 空字符串 / 全空格 都返回 null */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
