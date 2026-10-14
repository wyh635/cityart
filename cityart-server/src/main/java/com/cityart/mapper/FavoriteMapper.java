package com.cityart.mapper;

import com.cityart.vo.FavoriteExhibitionVO;
import com.cityart.vo.FavoriteGalleryVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收藏 Mapper
 * <p>
 * 简单 SQL（插入 / 删除 / count）用注解，带 JOIN 的列表查询写在 XML：FavoriteMapper.xml
 */
@Mapper
public interface FavoriteMapper {

    /**
     * 添加收藏：INSERT IGNORE + uk_user_target 唯一索引做幂等，
     * 重复收藏不会报错，影响行数为 0 也当成功
     */
    @Insert("insert ignore into favorite(user_id, target_type, target_id, create_time) " +
            "values(#{userId}, #{targetType}, #{targetId}, now())")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("targetType") Integer targetType,
                     @Param("targetId") Long targetId);

    /**
     * 取消收藏
     */
    @Delete("delete from favorite where user_id = #{userId} and target_type = #{targetType} and target_id = #{targetId}")
    int delete(@Param("userId") Long userId,
               @Param("targetType") Integer targetType,
               @Param("targetId") Long targetId);

    /**
     * 是否已收藏（详情页按钮状态）
     */
    @Select("select count(*) from favorite where user_id = #{userId} and target_type = #{targetType} and target_id = #{targetId}")
    int countByUserAndTarget(@Param("userId") Long userId,
                             @Param("targetType") Integer targetType,
                             @Param("targetId") Long targetId);

    /**
     * 某类收藏的总数（收藏列表的 tab 徽标、个人中心的「我的收藏」）
     */
    @Select("select count(*) from favorite where user_id = #{userId} and target_type = #{targetType}")
    int countByUserAndType(@Param("userId") Long userId, @Param("targetType") Integer targetType);

    /** 收藏的展览列表（JOIN 见 XML） */
    List<FavoriteExhibitionVO> listFavoriteExhibitions(@Param("userId") Long userId,
                                                       @Param("offset") int offset,
                                                       @Param("pageSize") int pageSize);

    /** 收藏的美术馆列表（JOIN 见 XML） */
    List<FavoriteGalleryVO> listFavoriteGalleries(@Param("userId") Long userId,
                                                  @Param("offset") int offset,
                                                  @Param("pageSize") int pageSize);
}
