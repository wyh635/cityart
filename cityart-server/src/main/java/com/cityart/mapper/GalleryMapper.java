package com.cityart.mapper;

import com.cityart.dto.AdminGalleryPageQueryDTO;
import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.vo.AdminGalleryDetailVO;
import com.cityart.vo.AdminGalleryVO;
import com.cityart.vo.GalleryDetailVO;
import com.cityart.vo.GalleryOptionVO;
import com.cityart.vo.GalleryPageVO;
import com.cityart.vo.GalleryVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 重算并回写某美术馆的「当前在展数量」
     * <p>
     * exhibition_count 是冗余字段（首页卡片直接读，避免 COUNT 子查询），
     * 新增 / 删除 / 编辑展览后必须调本方法同步，否则首页数字会一直是旧的。
     * <p>
     * 口径：该馆名下「今天在展期内」的展览数
     */
    @Update("update gallery g set g.exhibition_count = (" +
            "  select count(*) from exhibition e " +
            "  where e.gallery_id = g.id " +
            "    and e.start_date <= curdate() and e.end_date >= curdate()" +
            ") where g.id = #{id}")
    int syncExhibitionCount(@Param("id") Long id);

    /**
     * 校准所有美术馆的在展数量（全表重算，只更新对不上的行）
     * <p>
     * 用于定时校准：展览自然到期（日期过了）时没有人主动同步，
     * 该馆的 exhibition_count 会一直停在旧值
     *
     * @return 实际被修正的行数
     */
    @Update("update gallery g set g.exhibition_count = (" +
            "  select count(*) from exhibition e " +
            "  where e.gallery_id = g.id " +
            "    and e.start_date <= curdate() and e.end_date >= curdate()" +
            ") where g.exhibition_count <> (" +
            "  select count(*) from exhibition e " +
            "  where e.gallery_id = g.id " +
            "    and e.start_date <= curdate() and e.end_date >= curdate()" +
            ")")
    int repairAllExhibitionCount();

    // ==================================================================
    // 以下是后台「展馆管理」用的方法
    // ==================================================================

    /**
     * 后台展馆列表：总条数（条件与列表共用同一段 XML SQL）
     */
    long countAdminGalleryPage(@Param("dto") AdminGalleryPageQueryDTO dto);

    /**
     * 后台展馆列表：当前页数据
     */
    List<AdminGalleryVO> listAdminGalleryPage(@Param("dto") AdminGalleryPageQueryDTO dto);

    /**
     * 后台展馆详情（编辑回填，**不带 status=0 条件**：闭馆维护的馆也要能编辑）
     */
    @Select("select id, gallery_code, name, city, address, open_hour, close_hour, closed_day, " +
            "       open_time, cover_image, intro, type, exhibition_count, is_hot, status " +
            "from gallery where id = #{id}")
    AdminGalleryDetailVO getAdminGalleryById(@Param("id") Long id);

    /**
     * 新增展馆
     * <p>
     * open_time 由 Service 拼好后传进来（closedDay + openHour-closeHour）
     */
    @Insert("insert into gallery(gallery_code, name, city, address, open_hour, close_hour, closed_day, " +
            "  open_time, cover_image, intro, type, is_hot, status, exhibition_count, create_time) " +
            "values(#{galleryCode}, #{name}, #{city}, #{address}, #{openHour}, #{closeHour}, #{closedDay}, " +
            "  #{openTime}, #{coverImage}, #{intro}, #{type}, #{isHot}, #{status}, 0, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGallery(AdminGalleryDetailVO vo);

    /**
     * 编辑展馆（不含编号：编号生成后不可改）
     */
    @Update("update gallery set name = #{name}, city = #{city}, address = #{address}, " +
            "  open_hour = #{openHour}, close_hour = #{closeHour}, closed_day = #{closedDay}, " +
            "  open_time = #{openTime}, cover_image = #{coverImage}, intro = #{intro}, " +
            "  type = #{type}, is_hot = #{isHot}, status = #{status} " +
            "where id = #{id}")
    int updateGallery(AdminGalleryDetailVO vo);

    /**
     * 删除展馆
     */
    @Delete("delete from gallery where id = #{id}")
    int deleteGallery(@Param("id") Long id);

    /**
     * 该展馆名下的展览数量（删除前的校验之一）
     */
    @Select("select count(*) from exhibition where gallery_id = #{id}")
    int countExhibitionsByGalleryId(@Param("id") Long id);

    /**
     * 该展馆名下的订单数量（删除前的校验之一）
     * <p>
     * 订单是财务数据，只要有过订单就不允许删展馆，否则订单里的场馆信息会悬空
     */
    @Select("select count(*) from exhibition_order where gallery_id = #{id}")
    int countOrdersByGalleryId(@Param("id") Long id);

    /**
     * 某城市已有展馆编号的最大序号，用于生成展馆编号（如南京 → NJ-G007）
     */
    @Select("select max(cast(substring(gallery_code, 6) as unsigned)) " +
            "from gallery where city = #{city} and gallery_code is not null")
    Integer getMaxGalleryCodeSeqOfCity(@Param("city") String city);

    /**
     * 「所属展馆」下拉（新增展览页）
     * <p>
     * ⚠️ 不筛 status：闭馆维护的馆也可能已经挂了展览，后台编辑时要能选到它
     *
     * @param city 为 null 表示不按城市筛选
     */
    @Select("<script>" +
            "select id, name, city from gallery " +
            "<where>" +
            "  <if test='city != null and city != \"\"'> city = #{city} </if>" +
            "</where>" +
            " order by city asc, id asc" +
            "</script>")
    List<GalleryOptionVO> listGalleryOptions(@Param("city") String city);
}
