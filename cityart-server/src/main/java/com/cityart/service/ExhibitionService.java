package com.cityart.service;

import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.dto.ExhibitionWorkPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.ExhibitionPageVO;
import com.cityart.vo.ExhibitionWorkVO;

/**
 * 展览业务（全部展览列表 + 展览详情 + 作品集）
 */
public interface ExhibitionService {

    /**
     * 分页查询展览列表（正在展出 / 即将展出 / 往期展出共用）
     */
    PageResult<ExhibitionPageVO> pageQuery(ExhibitionPageQueryDTO dto);

    /**
     * 展览详情（详情页主体数据）
     *
     * @param id 展览 ID，查不到抛「展览不存在」
     */
    ExhibitionDetailVO getDetailById(Long id);

    /**
     * 展览作品集分页列表（详情页「作品集」区块独立分页）
     */
    PageResult<ExhibitionWorkVO> pageWorks(Long exhibitionId, ExhibitionWorkPageQueryDTO dto);
}
