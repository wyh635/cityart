package com.cityart.mapper;

import com.cityart.vo.ExhibitionWorkVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 展览作品集 Mapper
 * <p>
 * SQL 都是静态的（只有 WHERE + 排序 + LIMIT，没有动态条件），所以直接用注解写
 */
@Mapper
public interface ExhibitionWorkMapper {

    /**
     * 某场展览的作品总数（详情接口的 worksCount 用它）
     */
    @Select("select count(*) from exhibition_work where exhibition_id = #{exhibitionId}")
    long countByExhibitionId(@Param("exhibitionId") Long exhibitionId);

    /**
     * 某场展览的作品分页列表
     * ORDER BY 末尾的 id asc 不能省，否则 sort 有重复值时翻页会重复
     */
    @Select("select id, name, author, image, description from exhibition_work " +
            "where exhibition_id = #{exhibitionId} " +
            "order by sort desc, id asc " +
            "limit #{offset}, #{pageSize}")
    List<ExhibitionWorkVO> listByExhibitionId(@Param("exhibitionId") Long exhibitionId,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);
}
