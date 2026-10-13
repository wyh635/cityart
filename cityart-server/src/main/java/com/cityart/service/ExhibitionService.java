package com.cityart.service;

import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.ExhibitionPageVO;

/**
 * 「全部展览」列表业务
 */
public interface ExhibitionService {

    /**
     * 分页查询展览列表（正在展出 / 即将展出 / 往期展出共用）
     */
    PageResult<ExhibitionPageVO> pageQuery(ExhibitionPageQueryDTO dto);
}
