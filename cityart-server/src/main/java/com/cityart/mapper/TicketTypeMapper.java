package com.cityart.mapper;

import com.cityart.entity.TicketType;
import com.cityart.vo.TicketTypeVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 票种 Mapper
 * <p>
 * SQL 都是静态的（没有动态条件、没有 JOIN），所以全部用注解写在方法上
 */
@Mapper
public interface TicketTypeMapper {

    /**
     * 某展览在售的票种列表（remaining 由 SQL 直接算出来，不给前端透露 total/sold）
     */
    @Select("select id, name, price, total_stock - sold_count as remaining, limit_per_user " +
            "from ticket_type " +
            "where exhibition_id = #{exhibitionId} and status = 0 " +
            "order by sort desc, id asc")
    List<TicketTypeVO> listByExhibitionId(@Param("exhibitionId") Long exhibitionId);

    /**
     * 按 ID 查票种（下单时校验票种、拿价格与限购数）
     */
    @Select("select * from ticket_type where id = #{id}")
    TicketType getById(@Param("id") Long id);

    /**
     * 扣减票种库存：**条件更新 + 判断影响行数**，不用「先查再改」，否则并发会超卖
     *
     * @return 影响行数，0 表示库存不足（调用方要抛异常并回滚事务）
     */
    @Update("update ticket_type set sold_count = sold_count + #{quantity} " +
            "where id = #{id} and status = 0 and total_stock - sold_count >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 释放票种库存（取消 / 退款时回滚），加上 sold_count >= quantity 防止扣成负数
     */
    @Update("update ticket_type set sold_count = sold_count - #{quantity} " +
            "where id = #{id} and sold_count >= #{quantity}")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    // ==================================================================
    // 以下是后台「新增 / 编辑展览」用的方法
    // ==================================================================

    /**
     * 某展览的全部票种（含停售的），后台编辑展览时回填四个票价输入框
     */
    @Select("select * from ticket_type where exhibition_id = #{exhibitionId} order by sort desc, id asc")
    List<TicketType> listAllByExhibitionId(@Param("exhibitionId") Long exhibitionId);

    /**
     * 新增票种
     */
    @Insert("insert into ticket_type(exhibition_id, name, price, total_stock, sold_count, " +
            "  limit_per_user, sort, status, create_time) " +
            "values(#{exhibitionId}, #{name}, #{price}, #{totalStock}, 0, " +
            "  #{limitPerUser}, #{sort}, 0, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTicketType(TicketType ticketType);

    /**
     * 修改票种（名称 / 价格 / 库存 / 限购）
     * <p>
     * ⚠️ total_stock 不得小于 sold_count，否则剩余库存会变负数
     */
    @Update("update ticket_type set name = #{name}, price = #{price}, " +
            "  total_stock = #{totalStock}, limit_per_user = #{limitPerUser}, sort = #{sort} " +
            "where id = #{id} and #{totalStock} >= sold_count")
    int updateTicketType(TicketType ticketType);

    /**
     * 停售票种（已被订单引用的票种不能物理删除，只能停售）
     */
    @Update("update ticket_type set status = 1 where id = #{id}")
    int disableTicketType(@Param("id") Long id);

    /**
     * 某票种是否已被订单引用（决定能不能物理删除）
     */
    @Select("select count(*) from exhibition_order where ticket_type_id = #{ticketTypeId}")
    int countOrdersByTicketType(@Param("ticketTypeId") Long ticketTypeId);

    /**
     * 物理删除票种（仅限从未被下单引用过的）
     */
    @Delete("delete from ticket_type where id = #{id}")
    int deleteTicketType(@Param("id") Long id);
}
