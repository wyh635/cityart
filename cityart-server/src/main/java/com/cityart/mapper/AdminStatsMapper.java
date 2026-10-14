package com.cityart.mapper;

import com.cityart.dto.AdminStatsQueryDTO;
import com.cityart.vo.DashboardOverviewVO;
import com.cityart.vo.ExhibitionRankVO;
import com.cityart.vo.TicketStatVO;
import com.cityart.vo.TicketStatsSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台统计 Mapper（数据看板 + 票务数据报表）
 * <p>
 * SQL 全部写在 XML 里：src/main/resources/mapper/AdminStatsMapper.xml
 * （因为都是「多条件 + 分组聚合」的动态 SQL，用注解写会很难读）
 * <p>
 * 口径统一：售出 / 营收只统计 {@code status IN (2,5)}（已支付、已完成）；
 * 待支付未付、已取消、已退款都不计入。
 */
@Mapper
public interface AdminStatsMapper {

    // ==================================================================
    // 数据看板
    // ==================================================================

    /**
     * 指定日期范围内「已核销」的票数（今日到馆总人数）
     *
     * @param start 含
     * @param end   不含
     */
    int countVisitByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 指定范围内「售出票数 + 营业额」
     * <p>
     * 用 pay_time 而不是 create_time：营收要按「支付时间」算
     */
    DashboardOverviewVO sumSoldAndRevenue(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("city") String city,
                                          @Param("type") Integer type);

    /**
     * 展览排行：总条数
     */
    long countExhibitionRank(@Param("dto") AdminStatsQueryDTO dto);

    /**
     * 展览排行：当前页数据（不含客流占比，占比由 Service 算）
     */
    List<ExhibitionRankVO> listExhibitionRank(@Param("dto") AdminStatsQueryDTO dto);

    /**
     * 排行用的分母：该筛选条件下的**全部**售票总数
     * <p>
     * 🔑 客流占比必须在 Service 层算：SQL 里做除法要处理分母为 0 的边界，
     * 而且分页后当页之和并不是全量之和
     */
    int sumAllTicketsForRank(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             @Param("city") String city,
                             @Param("type") Integer type);

    // ==================================================================
    // 票务数据报表
    // ==================================================================

    /**
     * 票务数据：总条数（按「支付日 + 展览」分组的组数）
     */
    long countTicketStats(@Param("dto") AdminStatsQueryDTO dto);

    /**
     * 票务数据：当前页（按「支付日 + 展览」分组）
     */
    List<TicketStatVO> listTicketStats(@Param("dto") AdminStatsQueryDTO dto);

    /**
     * 票务数据汇总：总售票数 / 总核销数 / 今日营业额
     */
    TicketStatsSummaryVO getTicketStatsSummary(@Param("dto") AdminStatsQueryDTO dto,
                                               @Param("todayStart") LocalDateTime todayStart,
                                               @Param("todayEnd") LocalDateTime todayEnd);

    /**
     * 按「核销日 + 展览」统计核销票数
     * <p>
     * 🔑 必须与「按支付日」分开查：核销日期可能和支付日期不是同一天，
     * 若都按支付日分组，会出现「今天卖的票算在今天核销」的口径错误。
     *
     * @return 每个元素含 exhibitionId + statDate + verifiedCount
     */
    List<TicketStatVO> listVerifiedByUseDate(@Param("dto") AdminStatsQueryDTO dto);
}
