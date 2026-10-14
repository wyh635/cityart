package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 收藏列表分页结果
 * <p>
 * 比通用分页多两个 tab 徽标数字：exhibitionCount / galleryCount。
 * 列表元素类型随 targetType 变（展览卡片或美术馆卡片），所以用泛型 T。
 */
@Data
@ApiModel("收藏列表分页VO")
public class FavoritePageVO<T> implements Serializable {

    private Long total;

    private Integer pages;

    private Integer page;

    private Integer pageSize;

    @ApiModelProperty("收藏的展览总数（tab 徽标）")
    private Integer exhibitionCount;

    @ApiModelProperty("收藏的美术馆总数（tab 徽标）")
    private Integer galleryCount;

    private List<T> list;

    public static <T> FavoritePageVO<T> of(long total, int page, int pageSize, List<T> list,
                                          int exhibitionCount, int galleryCount) {
        FavoritePageVO<T> vo = new FavoritePageVO<>();
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize));
        vo.setList(list);
        vo.setExhibitionCount(exhibitionCount);
        vo.setGalleryCount(galleryCount);
        return vo;
    }
}
