package com.cityart.service.impl;

import com.cityart.enumeration.ExhibitionTypeEnum;
import com.cityart.enumeration.GalleryTypeEnum;
import com.cityart.mapper.CityMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.DictService;
import com.cityart.vo.DictItemVO;
import com.cityart.vo.GalleryOptionVO;
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

    @Autowired
    private CityMapper cityMapper;

    // ================= C 端 =================

    @Override
    public List<DictItemVO> listExhibitionTypes() {
        // 返回顺序：value 从大到小（5 摄影展览 → 1 综合展览），与 C 端接口文档的返回示例一致
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

    // ================= B 端后台字典 =================

    @Override
    public List<DictItemVO> listExhibitionTypesAsc() {
        // 后台接口文档的返回示例是 1→5 正序，与 C 端相反，所以这里不 reversed
        return Arrays.stream(ExhibitionTypeEnum.values())
                .sorted(Comparator.comparing(ExhibitionTypeEnum::getValue))
                .map(item -> new DictItemVO(item.getValue(), item.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DictItemVO> listGalleryTypesAsc() {
        return Arrays.stream(GalleryTypeEnum.values())
                .sorted(Comparator.comparing(GalleryTypeEnum::getValue))
                .map(item -> new DictItemVO(item.getValue(), item.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listAdminCities() {
        // 查 city 表：运营还没建馆时也能选到城市（C 端查 gallery 会返回空列表）
        return cityMapper.listEnabledCityNames();
    }

    @Override
    public List<GalleryOptionVO> listGalleryOptions(String city) {
        String trimmed = city == null || city.trim().isEmpty() ? null : city.trim();
        // 只返回下拉需要的三个字段，不带 intro / address 等大字段
        return galleryMapper.listGalleryOptions(trimmed);
    }
}
