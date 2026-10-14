package com.cityart.service.impl;

import com.cityart.enumeration.ExhibitionTypeEnum;
import com.cityart.enumeration.GalleryTypeEnum;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.DictService;
import com.cityart.vo.DictItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private GalleryMapper galleryMapper;

    @Override
    public List<DictItemVO> listExhibitionTypes() {
        // 返回顺序：value 从大到小（5 摄影展览 → 1 综合展览），与接口文档的返回示例一致
        return Arrays.stream(ExhibitionTypeEnum.values())
                .sorted(Comparator.comparing(ExhibitionTypeEnum::getValue).reversed())
                .map(item -> new DictItemVO(item.getValue(), item.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DictItemVO> listGalleryTypes() {
        return Arrays.stream(GalleryTypeEnum.values())
                .sorted(Comparator.comparing(GalleryTypeEnum::getValue).reversed())
                .map(item -> new DictItemVO(item.getValue(), item.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listCities() {
        // 方案 A：直接查数据库里已有的城市，不用额外维护字典表，也不会选出没有美术馆的城市
        return galleryMapper.listCities();
    }
}
