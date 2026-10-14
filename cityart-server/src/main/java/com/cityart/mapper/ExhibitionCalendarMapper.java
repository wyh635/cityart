package com.cityart.mapper;

import com.cityart.entity.ExhibitionCalendar;
import com.cityart.vo.CalendarVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 观展日历 Mapper
 * <p>
 * 表里没有记录的日期就是「不发售」，所以查询只按表里已有的行返回
 */
@Mapper
public interface ExhibitionCalendarMapper {

    /**
     * 某展览今天以后的可预约日期（含「已售完」标记）
     */
    @Select("select calendar_date as `date`, " +
            "       total_stock - sold_count as remaining, " +
            "       case when total_stock - sold_count <= 0 or status = 1 then 1 else 0 end as sold_out " +
            "from exhibition_calendar " +
            "where exhibition_id = #{exhibitionId} and calendar_date >= curdate() " +
            "order by calendar_date asc")
    List<CalendarVO> listByExhibitionId(@Param("exhibitionId") Long exhibitionId);

    /**
     * 取某个展览某一天的库存记录（下单时校验「日期在不在日历里」）
     *
     * @return 查不到返回 null → 说明该日期不发售
     */
    @Select("select * from exhibition_calendar where exhibition_id = #{exhibitionId} and calendar_date = #{date}")
    ExhibitionCalendar getByDate(@Param("exhibitionId") Long exhibitionId, @Param("date") LocalDate date);

    /**
     * 扣减当日库存：条件更新 + 判断影响行数
     *
     * @return 影响行数，0 表示该日已售完
     */
    @Update("update exhibition_calendar set sold_count = sold_count + #{quantity} " +
            "where exhibition_id = #{exhibitionId} and calendar_date = #{date} " +
            "  and total_stock - sold_count >= #{quantity}")
    int decreaseStock(@Param("exhibitionId") Long exhibitionId,
                      @Param("date") LocalDate date,
                      @Param("quantity") Integer quantity);

    /**
     * 释放当日库存（取消 / 退款时回滚）
     */
    @Update("update exhibition_calendar set sold_count = sold_count - #{quantity} " +
            "where exhibition_id = #{exhibitionId} and calendar_date = #{date} " +
            "  and sold_count >= #{quantity}")
    int increaseStock(@Param("exhibitionId") Long exhibitionId,
                      @Param("date") LocalDate date,
                      @Param("quantity") Integer quantity);

    // ==================================================================
    // 以下是后台「新增 / 编辑展览」用的方法
    // ==================================================================

    /**
     * 批量生成日历库存：展期内每天一条，total_stock = 每日票数上限
     * <p>
     * 🔑 这是「每日票数上限」的落地点：后台只填一个数字，这里按展期天数展开成 N 行。
     * 必须用批量 insert（一条 SQL 多组 VALUES），不要在 Service 里循环调 N 次。
     *
     * @param dates 展期内的所有日期
     * @return 影响行数
     */
    @Insert("<script>" +
            "insert into exhibition_calendar(exhibition_id, calendar_date, total_stock, sold_count, status) values " +
            "<foreach collection='dates' item='d' separator=','>" +
            "  (#{exhibitionId}, #{d}, #{totalStock}, 0, 0)" +
            "</foreach>" +
            "</script>")
    int batchInsertCalendar(@Param("exhibitionId") Long exhibitionId,
                            @Param("dates") List<LocalDate> dates,
                            @Param("totalStock") Integer totalStock);

    /**
     * 删除某展览尚未开始发售的日历行（编辑展览缩短展期时清理）
     * <p>
     * ⚠️ 只删「还没卖出过」且「日期在未来」的行；
     * 已售出的行不能删（有订单引用），由调用方改成 status = 1 停售。
     */
    @Delete("delete from exhibition_calendar " +
            "where exhibition_id = #{exhibitionId} and sold_count = 0 and calendar_date > #{afterDate}")
    int deleteUnsoldCalendarAfter(@Param("exhibitionId") Long exhibitionId,
                                  @Param("afterDate") LocalDate afterDate);

    /**
     * 把超出展期的日历行置为「已约满/闭馆」，停止继续发售（已售出的行用这个方式下线）
     */
    @Update("update exhibition_calendar set status = 1 " +
            "where exhibition_id = #{exhibitionId} and calendar_date > #{afterDate} and status = 0")
    int closeCalendarAfter(@Param("exhibitionId") Long exhibitionId,
                           @Param("afterDate") LocalDate afterDate);

    /**
     * 批量调整今天及以后的每日库存（后台改「每日票数上限」时用）
     * <p>
     * 加上 #{totalStock} &gt;= sold_count：新库存不得小于已售数量，否则会退成负数
     *
     * @return 影响行数
     */
    @Update("update exhibition_calendar set total_stock = #{totalStock} " +
            "where exhibition_id = #{exhibitionId} and calendar_date >= curdate() " +
            "  and #{totalStock} >= sold_count")
    int updateDailyStock(@Param("exhibitionId") Long exhibitionId,
                         @Param("totalStock") Integer totalStock);

    /**
     * 精确调整某一天的库存
     * <p>
     * 条件里的 #{totalStock} &gt;= sold_count 是关键：不能让新库存小于已售数量
     *
     * @return 影响行数，0 表示该日不存在、或新库存小于已售数量
     */
    @Update("update exhibition_calendar set total_stock = #{totalStock} " +
            "where exhibition_id = #{exhibitionId} and calendar_date = #{date} " +
            "  and #{totalStock} >= sold_count")
    int updateStockByDate(@Param("exhibitionId") Long exhibitionId,
                          @Param("date") LocalDate date,
                          @Param("totalStock") Integer totalStock);

    /**
     * 某展览已售出的日历行数（占比统计 / 判断能不能删）
     */
    @Select("select count(*) from exhibition_calendar where exhibition_id = #{exhibitionId} and sold_count > 0")
    int countSoldCalendar(@Param("exhibitionId") Long exhibitionId);

    /**
     * 某展览已有的全部放票日期（编辑延长展期时，用来算哪些日期还没生成）
     */
    @Select("select calendar_date from exhibition_calendar where exhibition_id = #{exhibitionId}")
    List<LocalDate> listAllDates(@Param("exhibitionId") Long exhibitionId);

    /**
     * 删除某展览的全部日历行（仅在没有订单、删除展览时用）
     */
    @Delete("delete from exhibition_calendar where exhibition_id = #{exhibitionId}")
    int deleteAllByExhibitionId(@Param("exhibitionId") Long exhibitionId);

    /**
     * 校准所有日历行的已售数量（全表重算，只更新对不上的行）
     *
     * @return 实际被修正的行数
     */
    @Update("update exhibition_calendar c set c.sold_count = (" +
            "  select ifnull(sum(o.quantity), 0) from exhibition_order o " +
            "  where o.exhibition_id = c.exhibition_id " +
            "    and o.visit_date = c.calendar_date " +
            "    and o.status in (2,5)" +
            ") where c.sold_count <> (" +
            "  select ifnull(sum(o.quantity), 0) from exhibition_order o " +
            "  where o.exhibition_id = c.exhibition_id " +
            "    and o.visit_date = c.calendar_date " +
            "    and o.status in (2,5)" +
            ")")
    int repairAllSoldCount();
}
