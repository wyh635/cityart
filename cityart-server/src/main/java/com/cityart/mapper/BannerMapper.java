package com.cityart.mapper;

import com.cityart.vo.BannerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 首页轮播图 Mapper
 * <p>
 * SQL 很简单（没有动态条件、没有 JOIN），所以直接用注解写在方法上，
 * 不用单独建 XML 文件。有 <if> 动态条件或 JOIN 的 SQL 见 GalleryMapper.xml、ExhibitionMapper.xml。
 */
@Mapper
public interface BannerMapper {

    /**
     * 查询首页轮播图，按 sort 从大到小
     * 末尾用 id asc 兜底，防止 sort 相同时顺序不确定
     */
    @Select("select id, image_url, title from banner order by sort desc, id asc")
    List<BannerVO> listBanners();
}
