package com.cityart.mapper;

import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.vo.GalleryDetailVO;
import com.cityart.vo.GalleryPageVO;
import com.cityart.vo.GalleryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 美术馆 Mapper
 * <p>
 * 简单 SQL（没有动态条件）用注解写在方法上，复杂的（&lt;if&gt; 动态筛选 + 分页）写 XML：
 * src/main/resources/mapper/GalleryMapper.xml
 */
@Mapper
public interface GalleryMapper {

    /**
     * 首页「热门美术馆」
     * 条件：is_hot = 1（运营标记热门）且 status = 0（正常营业），city 可选
     *
     * @param city  城市，为 null 表示不筛选
     * @param limit 返回条数上限
     */
    List<GalleryVO> listHotGalleries(@Param("city") String city, @Param("limit") Integer limit);

    /**
     * 城市字典：只返回「有正常营业美术馆」的城市（方案 A，不用额外维护字典表）
     */
    @Select("select distinct city from gallery where status = 0 order by city")
    List<String> listCities();

    /**
     * 「美术馆」列表：总条数（分页用，条件和列表查询完全一致）
     */
    long countGalleryPage(@Param("dto") GalleryPageQueryDTO dto);

    /**
     * 「美术馆」列表：当前页数据
     */
    List<GalleryPageVO> listGalleryPage(@Param("dto") GalleryPageQueryDTO dto);

    /**
     * 美术馆详情：单条
     * 带 status = 0：闭馆维护的馆不对外展示
     *
     * @return 查不到返回 null，由 Service 抛「美术馆不存在」
     */
    @Select("select id, name, cover_image, address, city, open_time, intro, " +
            "type, exhibition_count, is_hot " +
            "from gallery where id = #{id} and status = 0")
    GalleryDetailVO getGalleryById(@Param("id") Long id);
}
