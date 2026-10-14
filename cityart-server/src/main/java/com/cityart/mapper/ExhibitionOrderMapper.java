package com.cityart.mapper;

import com.cityart.dto.OrderPageQueryDTO;
import com.cityart.dto.RecordPageQueryDTO;
import com.cityart.dto.TicketPageQueryDTO;
import com.cityart.dto.VerifyRecordPageQueryDTO;
import com.cityart.entity.ExhibitionOrder;
import com.cityart.vo.OrderDetailVO;
import com.cityart.vo.OrderPageItemVO;
import com.cityart.vo.OrderVerifyVO;
import com.cityart.vo.RecordVO;
import com.cityart.vo.TicketVO;
import com.cityart.vo.VerifyRecordVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper
 * <p>
 * 约定：**简单 SQL 用注解**（插入、条件更新、count），
 * **JOIN + 动态条件的查询写在 XML**：src/main/resources/mapper/ExhibitionOrderMapper.xml
 */
@Mapper
public interface ExhibitionOrderMapper {

    /**
     * 插入订单（order_no 由 OrderNoUtil 生成，不用自增 id）
     */
    @Insert("insert into exhibition_order(order_no, user_id, exhibition_id, gallery_id, ticket_type_id, " +
            "ticket_name, price, quantity, total_amount, status, visit_date, contact_name, contact_phone, " +
            "pay_expire_time, refund_status, create_time) " +
            "values(#{orderNo}, #{userId}, #{exhibitionId}, #{galleryId}, #{ticketTypeId}, " +
            "#{ticketName}, #{price}, #{quantity}, #{totalAmount}, #{status}, #{visitDate}, " +
            "#{contactName}, #{contactPhone}, #{payExpireTime}, #{refundStatus}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ExhibitionOrder order);

    /**
     * 按订单号查订单（支付回调没有用户上下文，只能按订单号查）
     */
    @Select("select * from exhibition_order where order_no = #{orderNo}")
    ExhibitionOrder getByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 支付成功：**幂等更新**，只有还是「待支付」的订单会被改成「已支付」
     * 重复回调时影响行数为 0，直接当成功处理即可
     */
    @Update("update exhibition_order " +
            "set status = 2, pay_type = #{payType}, pay_time = now(), " +
            "    qr_code = #{qrCode}, expire_time = #{expireTime} " +
            "where order_no = #{orderNo} and status = 1")
    int markPaid(@Param("orderNo") String orderNo,
                 @Param("payType") Integer payType,
                 @Param("qrCode") String qrCode,
                 @Param("expireTime") LocalDateTime expireTime);

    /**
     * 取消订单：条件更新防止重复取消（只有待支付能取消）
     */
    @Update("update exhibition_order " +
            "set status = 3, cancel_time = now(), cancel_reason = #{reason} " +
            "where order_no = #{orderNo} and user_id = #{userId} and status = 1")
    int cancel(@Param("orderNo") String orderNo,
               @Param("userId") Long userId,
               @Param("reason") String reason);

    // ==================================================================
    // 定时任务用的系统级方法（没有用户上下文，只能按订单号操作）
    // ==================================================================

    /**
     * 查所有「已超时且仍未支付」的订单（定时任务用）
     * <p>
     * 只取前 limit 条，避免一次捞出太多造成长事务
     *
     * @param limit 单次处理上限
     */
    @Select("select * from exhibition_order " +
            "where status = 1 and pay_expire_time is not null and pay_expire_time < now() " +
            "order by pay_expire_time asc " +
            "limit #{limit}")
    List<ExhibitionOrder> listTimeoutOrders(@Param("limit") int limit);

    /**
     * 系统取消订单：**不带 user_id 条件**（定时任务没有当前用户）
     * <p>
     * Status 仍限制为 1（待支付），保证与用户主动取消互斥；
     * 影响行数 0 说明这一瞬间用户已支付或已自行取消，跳过即可
     */
    @Update("update exhibition_order " +
            "set status = 3, cancel_time = now(), cancel_reason = #{reason} " +
            "where order_no = #{orderNo} and status = 1")
    int cancelBySystem(@Param("orderNo") String orderNo, @Param("reason") String reason);

    // ==================================================================
    // B 端扫码核销
    // ==================================================================

    /**
     * 核销：条件更新，只有「已支付(status=2)」能改成「已完成(status=5)」
     * <p>
     * 这是防重复核销的关键：两个核销员同时扫同一张票时，
     * 只有一条 UPDATE 能成功（影响行数 1），另一条得到 0 → 提示「该电子票已核销」。
     *
     * @return 影响行数，0 表示订单不是已支付状态（可能已核销 / 已退款 / 未支付）
     */
    @Update("update exhibition_order " +
            "set status = 5, use_time = now(), " +
            "    verify_type = #{verifyType}, verify_admin_id = #{adminId} " +
            "where order_no = #{orderNo} and status = 2")
    int verify(@Param("orderNo") String orderNo,
               @Param("verifyType") Integer verifyType,
               @Param("adminId") Long adminId);

    /**
     * 核销成功后取展示信息（订单 + 展览 + 美术馆）
     */
    @Select("select o.order_no, o.ticket_name, o.quantity, o.contact_name, " +
            "       o.visit_date, o.use_time, o.verify_type, o.user_id, " +
            "       e.title as exhibition_title, g.name as gallery_name " +
            "from exhibition_order o " +
            "join exhibition e on e.id = o.exhibition_id " +
            "join gallery g on g.id = o.gallery_id " +
            "where o.order_no = #{orderNo}")
    OrderVerifyVO getVerifyInfo(@Param("orderNo") String orderNo);

    /**
     * 核销记录：总条数（条件与列表查询共用同一段 SQL）
     */
    long countVerifyRecords(@Param("dto") VerifyRecordPageQueryDTO dto);

    /**
     * 核销记录：当前页数据（JOIN 见 XML）
     */
    List<VerifyRecordVO> listVerifyRecords(@Param("dto") VerifyRecordPageQueryDTO dto);

    /**
     * 退款：只有已支付且未退款的订单能退；refund_amount 直接取订单总额
     */
    @Update("update exhibition_order " +
            "set status = 4, refund_status = 2, refund_amount = total_amount " +
            "where order_no = #{orderNo} and user_id = #{userId} and status = 2 and refund_status = 0")
    int refund(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    /**
     * 该用户在这场展览上已购的有效票数（限购校验用）
     * <p>
     * 只统计「待支付、已支付、已完成」，已取消/已退款的订单不占限购额度
     */
    @Select("select ifnull(sum(quantity), 0) from exhibition_order " +
            "where user_id = #{userId} and exhibition_id = #{exhibitionId} and status in (1, 2, 5)")
    int sumUserQuantity(@Param("userId") Long userId, @Param("exhibitionId") Long exhibitionId);

    /**
     * 有效票夹数量（已支付订单数），票夹页的 total / validCount 和「个人中心」都用它
     */
    @Select("select count(*) from exhibition_order where user_id = #{userId} and status = 2")
    int countValidTickets(@Param("userId") Long userId);

    /**
     * 看展次数（已核销订单数）
     */
    @Select("select count(*) from exhibition_order where user_id = #{userId} and use_time is not null")
    int countVisited(@Param("userId") Long userId);

    // ===== 下面 5 个查询有 JOIN / 动态条件，SQL 写在 XML 里 =====

    /** 订单详情（支付页 / 订单详情页） */
    OrderDetailVO getOrderDetail(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    /** 我的订单：总条数 */
    long countOrderPage(@Param("userId") Long userId, @Param("dto") OrderPageQueryDTO dto);

    /** 我的订单：当前页数据 */
    List<OrderPageItemVO> listOrderPage(@Param("userId") Long userId, @Param("dto") OrderPageQueryDTO dto);

    /** 票夹：当前页数据（数据源是已支付订单） */
    List<TicketVO> listTickets(@Param("userId") Long userId, @Param("dto") TicketPageQueryDTO dto);

    /** 看展记录：总条数 */
    long countRecords(@Param("userId") Long userId, @Param("dto") RecordPageQueryDTO dto);

    /** 看展记录：当前页数据（数据源是已核销订单） */
    List<RecordVO> listRecords(@Param("userId") Long userId, @Param("dto") RecordPageQueryDTO dto);
}
