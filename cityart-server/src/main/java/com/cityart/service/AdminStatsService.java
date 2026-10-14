package com.cityart.service;

import com.cityart.dto.AdminStatsQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.DashboardOverviewVO;
import com.cityart.vo.ExhibitionRankVO;
import com.cityart.vo.TicketStatVO;
import com.cityart.vo.TicketStatsSummaryVO;

import java.util.List;

/**
 * B 端统计业务（数据看板 + 票务数据报表）
 */
public interface AdminStatsService {

    // ================= 数据看板 =================

    /**
     * 看板顶部三张卡片
     */
    DashboardOverviewVO overview(AdminStatsQueryDTO dto);

    /**
     * 展览排行（含客流占比）
     */
    PageResult<ExhibitionRankVO> exhibitionRank(AdminStatsQueryDTO dto);

    // ================= 票务数据报表 =================

    /**
     * 票务数据表格（按「统计日期 + 展览」分组）
     */
    PageResult<TicketStatVO> ticketStats(AdminStatsQueryDTO dto);

    /**
     * 票务数据底部三张卡片
     */
    TicketStatsSummaryVO ticketStatsSummary(AdminStatsQueryDTO dto);

    /**
     * 导出票务数据（不分页，返回全部行）
     */
    List<TicketStatVO> exportTicketStats(AdminStatsQueryDTO dto);
}
