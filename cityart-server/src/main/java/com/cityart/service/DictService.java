package com.cityart.service;

import com.cityart.vo.DictItemVO;

import java.util.List;

/**
 * 字典接口业务（展览类型 / 美术馆类型 / 城市）
 */
public interface DictService {

    /** 展览类型字典（1-5） */
    List<DictItemVO> listExhibitionTypes();

    /** 美术馆类型字典（1-5） */
    List<DictItemVO> listGalleryTypes();

    /** 城市字典：只返回有正常营业美术馆的城市 */
    List<String> listCities();
}
