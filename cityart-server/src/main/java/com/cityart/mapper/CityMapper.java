package com.cityart.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 城市 Mapper（对应 city 表）
 * <p>
 * 与 C 端的城市字典不同：
 * <ul>
 *     <li>C 端：{@code SELECT DISTINCT city FROM gallery}——只列「有正常营业美术馆」的城市</li>
 *     <li>B 端：查本表，运营在一个馆都还没建时也能选到城市</li>
 * </ul>
 */
@Mapper
public interface CityMapper {

    /**
     * 启用的城市名列表（后台下拉用）
     * <p>
     * 按 sort 倒序：sort 数字越大越靠前（如南京 100 → 苏州 90 → 无锡 80）
     */
    @Select("select name from city where status = 0 order by sort desc, id asc")
    List<String> listEnabledCityNames();
}
