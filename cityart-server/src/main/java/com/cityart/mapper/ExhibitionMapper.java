package com.cityart.mapper;

import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.dto.GalleryExhibitionPageQueryDTO;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.ExhibitionPageVO;
import com.cityart.vo.ExhibitionVO;
import com.cityart.vo.GalleryExhibitionCountVO;
import com.cityart.vo.GalleryExhibitionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 展览 Mapper
 * <p>
 * 所有 SQL 都写在 XML 里：src/main/resources/mapper/ExhibitionMapper.xml
 * （字段映射靠 application.yml 的 map-underscore-to-camel-case: true）
 * <p>
 * 方法名 ↔ XML 里的 statement id 必须一一对应；
 * XML 里用 #{dto.xxx} 取值，所以参数上要加 @Param("dto")，否则 MyBatis 找不到 dto 这个属性。
 */
@Mapper
public interface ExhibitionMapper {

    /**
     * 首页「当前热门展览」：is_hot = 1 且今天在展期内
     *
     * @param city  城市（按所属美术馆的城市匹配），为 null 表示不筛选
     * @param limit 返回条数上限
     */
    List<ExhibitionVO> listHotExhibitions(@Param("city") String city, @Param("limit") Integer limit);

    /**
     * 「全部展览」列表：总条数（分页用，条件和列表查询完全一致）
     */
    long countExhibitionPage(@Param("dto") ExhibitionPageQueryDTO dto);

    /**
     * 「全部展览」列表：当前页数据
     */
    List<ExhibitionPageVO> listExhibitionPage(@Param("dto") ExhibitionPageQueryDTO dto);

    /**
     * 展览详情：单条，JOIN gallery 取美术馆名称与地址
     *
     * @return 查不到返回 null，由 Service 抛「展览不存在」
     */
    ExhibitionDetailVO getExhibitionDetailById(@Param("id") Long id);

    /**
     * 某美术馆下「正在展出 / 往期展出」的数量（美术馆详情页分组标题用）
     */
    GalleryExhibitionCountVO getGalleryExhibitionCount(@Param("galleryId") Long galleryId);

    /**
     * 美术馆详情页的展览列表：总条数
     */
    long countGalleryExhibitions(@Param("galleryId") Long galleryId,
                                 @Param("dto") GalleryExhibitionPageQueryDTO dto);

    /**
     * 美术馆详情页的展览列表：当前页数据
     */
    List<GalleryExhibitionVO> listGalleryExhibitions(@Param("galleryId") Long galleryId,
                                                     @Param("dto") GalleryExhibitionPageQueryDTO dto);
}
