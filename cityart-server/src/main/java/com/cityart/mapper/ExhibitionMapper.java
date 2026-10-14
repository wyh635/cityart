package com.cityart.mapper;

import com.cityart.dto.AdminExhibitionPageQueryDTO;
import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.dto.GalleryExhibitionPageQueryDTO;
import com.cityart.vo.AdminExhibitionDetailVO;
import com.cityart.vo.AdminExhibitionVO;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.ExhibitionPageVO;
import com.cityart.vo.ExhibitionVO;
import com.cityart.vo.GalleryExhibitionCountVO;
import com.cityart.vo.GalleryExhibitionVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
     * 取展览所属美术馆ID（下单时写进订单的快照字段）
     * <p>
     * SQL 简单（单表单列），所以用注解；返回 null 说明展览不存在
     */
    @Select("select gallery_id from exhibition where id = #{id}")
    Long getGalleryIdById(@Param("id") Long id);

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

    // ==================================================================
    // 以下是后台「展览管理」用的方法
    // ==================================================================

    /**
     * 后台展览列表：总条数（条件与列表查询共用同一段 SQL，保证分页页码正确）
     */
    long countAdminExhibitionPage(@Param("dto") AdminExhibitionPageQueryDTO dto);

    /**
     * 后台展览列表：当前页数据
     * <p>
     * ⚠️ 不含「当日库存」，那是由 Service 批量查日历表回填的（避免 N+1）
     */
    List<AdminExhibitionVO> listAdminExhibitionPage(@Param("dto") AdminExhibitionPageQueryDTO dto);

    /**
     * 后台展览详情（编辑回填用）
     *
     * @return 查不到返回 null
     */
    AdminExhibitionDetailVO getAdminExhibitionById(@Param("id") Long id);

    /**
     * 新增展览
     * <p>
     * ⚠️ 不写 price / total_stock：这两列是「展示用」的冗余值，
     * 要等票种插完后由 {@link #updateDisplayPriceAndStock} 回写（取最低价 + 票种库存合计）。
     * 建表时 price 允许 NULL、total_stock 默认 0，所以先空着是合法的。
     */
    @Insert("insert into exhibition(gallery_id, exhibition_code, title, poster_image, images, " +
            "  start_date, end_date, open_time, description, is_hot, sort_order, type, " +
            "  sold_count, daily_stock_limit, status, create_time) " +
            "values(#{galleryId}, #{exhibitionCode}, #{title}, #{posterImage}, #{images}, " +
            "  #{startDate}, #{endDate}, #{openTime}, #{description}, #{isHot}, #{sortOrder}, #{type}, " +
            "  0, #{dailyStockLimit}, 0, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertExhibition(AdminExhibitionDetailVO vo);

    /**
     * 修改展览（不含编号：编号生成后不可改）
     */
    @Update("update exhibition set gallery_id = #{galleryId}, title = #{title}, " +
            "  poster_image = #{posterImage}, images = #{images}, " +
            "  start_date = #{startDate}, end_date = #{endDate}, open_time = #{openTime}, " +
            "  description = #{description}, type = #{type}, daily_stock_limit = #{dailyStockLimit}, " +
            "  is_hot = #{isHot}, sort_order = #{sortOrder} " +
            "where id = #{id}")
    int updateExhibition(AdminExhibitionDetailVO vo);

    /**
     * 上下架 / 热门开关（动态 SQL：只更新传了的字段）
     */
    @Update("<script>" +
            "update exhibition " +
            "<set>" +
            "  <if test='status != null'> status = #{status}, </if>" +
            "  <if test='isHot != null'> is_hot = #{isHot}, </if>" +
            "  <if test='sortOrder != null'> sort_order = #{sortOrder}, </if>" +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("isHot") Integer isHot,
                     @Param("sortOrder") Integer sortOrder);

    /**
     * 删除展览
     */
    @Delete("delete from exhibition where id = #{id}")
    int deleteExhibition(@Param("id") Long id);

    /**
     * 该展览的有效订单数（待支付 / 已支付 / 已完成）
     * <p>
     * >0 时禁止删除展览 —— 订单是财务数据，不能因为删展览就悬空
     */
    @Select("select count(*) from exhibition_order " +
            "where exhibition_id = #{id} and status in (1, 2, 5)")
    int countActiveOrders(@Param("id") Long id);

    /**
     * 某城市下已有展览数量 + 最大编号，用于生成展览编号（如 NJ007）
     * <p>
     * 取该城市所有展览编号里数字最大的那个，Service 再 +1
     */
    @Select("select max(cast(substring(exhibition_code, 3) as unsigned)) " +
            "from exhibition e join gallery g on g.id = e.gallery_id " +
            "where g.city = #{city} and e.exhibition_code is not null")
    Integer getMaxCodeSeqOfCity(@Param("city") String city);

    /**
     * 该展览的日历天数（确认「每日票数上限」是否已生成）
     */
    @Select("select count(*) from exhibition_calendar where exhibition_id = #{id}")
    int countCalendar(@Param("id") Long id);

    /**
     * 批量取各展览的「最近放票日剩余库存」，用于后台列表的「当日库存」列
     * <p>
     * 一次查全部展览，避免在循环里逐条查日历表（N+1）
     *
     * @return 每个元素的 exhibitionId + dailyStock
     */
    List<AdminExhibitionVO> listNearestDailyStock(@Param("ids") List<Long> ids);

    /**
     * 回写展览的展示价与总库存（新增/编辑票种后同步，供 C 端列表展示）
     * <p>
     * price 取所有票种的最低价；total_stock 取各票种库存之和
     */
    @Update("update exhibition set price = #{price}, total_stock = #{totalStock} where id = #{id}")
    int updateDisplayPriceAndStock(@Param("id") Long id,
                                   @Param("price") java.math.BigDecimal price,
                                   @Param("totalStock") Integer totalStock);

    /**
     * 只改「每日票数上限」字段（后台调整库存时同步展示用）
     */
    @Update("update exhibition set daily_stock_limit = #{dailyStockLimit} where id = #{id}")
    int updateDailyStockLimit(@Param("id") Long id, @Param("dailyStockLimit") Integer dailyStockLimit);

    // ==================================================================
    // 冗余字段校准（定时任务 / 手工修复用，全表重算，幂等）
    // ==================================================================

    /**
     * 校准展览的已售数量、总库存、展示价
     * <p>
     * 口径：
     * <ul>
     *     <li>sold_count ← Σ 订单数量（status IN (2,5)）</li>
     *     <li>total_stock ← Σ 在售票种库存</li>
     *     <li>price ← MIN(在售票种价格)</li>
     * </ul>
     * 对应 db/08_repair_denormalized.sql
     *
     * @return 实际被修正的行数
     */
    @Update("update exhibition e " +
            "set e.sold_count = (" +
            "      select ifnull(sum(o.quantity), 0) from exhibition_order o " +
            "      where o.exhibition_id = e.id and o.status in (2,5)" +
            "    ), " +
            "    e.total_stock = ifnull((" +
            "      select sum(t.total_stock) from ticket_type t " +
            "      where t.exhibition_id = e.id and t.status = 0" +
            "    ), e.total_stock), " +
            "    e.price = ifnull((" +
            "      select min(t.price) from ticket_type t " +
            "      where t.exhibition_id = e.id and t.status = 0" +
            "    ), e.price) " +
            "where e.sold_count <> (" +
            "      select ifnull(sum(o.quantity), 0) from exhibition_order o " +
            "      where o.exhibition_id = e.id and o.status in (2,5)" +
            "    )")
    int repairExhibitionDenormalized();

    /**
     * 校准票种的已售数量
     */
    @Update("update ticket_type t " +
            "set t.sold_count = (" +
            "      select ifnull(sum(o.quantity), 0) from exhibition_order o " +
            "      where o.ticket_type_id = t.id and o.status in (2,5)" +
            "    ) " +
            "where t.sold_count <> (" +
            "      select ifnull(sum(o.quantity), 0) from exhibition_order o " +
            "      where o.ticket_type_id = t.id and o.status in (2,5)" +
            "    )")
    int repairTicketTypeSoldCount();
}
