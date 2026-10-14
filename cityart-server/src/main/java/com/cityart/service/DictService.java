package com.cityart.service;

import com.cityart.vo.DictItemVO;
import com.cityart.vo.GalleryOptionVO;

import java.util.List;

/**
 * 字典接口业务（展览类型 / 美术馆类型 / 城市 / 展馆下拉）
 * <p>
 * C 端与 B 端共用类型枚举，但**排序方向不同**：
 * C 端按 value 倒序（5→1，与 C 端接口文档示例一致），
 * B 端按 value 正序（1→5，与后台接口文档示例一致）。
 */
public interface DictService {

    // ================= C 端 =================

    /** 展览类型字典（1-5，按 value 倒序） */
    List<DictItemVO> listExhibitionTypes();

    /** 美术馆类型字典（1-5，按 value 倒序） */
    List<DictItemVO> listGalleryTypes();

    /** 城市字典：只返回有正常营业美术馆的城市 */
    List<String> listCities();

    // ================= B 端后台字典 =================

    /** 展览类型字典（按 value 正序 1→5） */
    List<DictItemVO> listExhibitionTypesAsc();

    /** 美术馆类型字典（按 value 正序 1→5） */
    List<DictItemVO> listGalleryTypesAsc();

    /**
     * 城市字典（B 端）
     * <p>
     * 查 city 表而不是 DISTINCT gallery.city —— 运营还没建任何展馆时也要能选到城市
     */
    List<String> listAdminCities();

    /**
     * 「所属展馆」下拉（新增展览页的「所属展馆 *」）
     *
     * @param city 可选，选了城市就只列该城市的馆
     */
    List<GalleryOptionVO> listGalleryOptions(String city);
}
