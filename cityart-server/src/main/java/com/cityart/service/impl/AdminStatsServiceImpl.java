package com.cityart.service.impl;

import com.cityart.constant.MessageConstant;
import com.cityart.dto.AdminStatsQueryDTO;
import com.cityart.exception.BaseException;
import com.cityart.mapper.AdminStatsMapper;
import com.cityart.result.PageResult;
import com.cityart.service.AdminStatsService;
import com.cityart.vo.DashboardOverviewVO;
import com.cityart.vo.ExhibitionRankVO;
import com.cityart.vo.TicketStatVO;
import com.cityart.vo.TicketStatsSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B 端统计实现
 */
@Service
@Slf4j
public class AdminStatsServiceImpl implements AdminStatsService {

    @Autowired
    private AdminStatsMapper statsMapper;

    // ==================================================================
    // 数据看板
    // ==================================================================

    @Override
    public DashboardOverviewVO overview(AdminStatsQueryDTO dto) {
        prepare(dto);

        // ① 到馆人数：按核销日统计（口径固定为今天，不受 rangeType 影响）
        //    原型卡片文案就是「今日到馆总人数」
        LocalDate today = LocalDate.now();
        int visitCount = statsMapper.countVisitByRange(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        // ② 售票总量 + 营业额：按 rangeType 解析出的范围统计
        DashboardOverviewVO sum = statsMapper.sumSoldAndRevenue(
                dto.getStartTime(), dto.getEndTime(), dto.getCity(), dto.getType());

        return DashboardOverviewVO.builder()
                .todayVisitCount(visitCount)
                .weekTicketCount(sum == null || sum.getWeekTicketCount() == null ? 0 : sum.getWeekTicketCount())
                .weekRevenue(sum == null || sum.getWeekRevenue() == null
                        ? java.math.BigDecimal.ZERO : sum.getWeekRevenue())
                .refreshTime(LocalDateTime.now())
                .build();
    }

    @Override
    public PageResult<ExhibitionRankVO> exhibitionRank(AdminStatsQueryDTO dto) {
        prepare(dto);

        long total = statsMapper.countExhibitionRank(dto);

        List<ExhibitionRankVO> list;
        if (total == 0) {
            list = new ArrayList<>();
        } else {
            list = statsMapper.listExhibitionRank(dto);

            // 🔑 客流占比在 Service 层算：
            // 分母是「该筛选条件下的全部售票总数」，不是当页之和
            int denominator = statsMapper.sumAllTicketsForRank(
                    dto.getStartTime(), dto.getEndTime(), dto.getCity(), dto.getType());

            int baseRank = dto.getOffset();
            for (int i = 0; i < list.size(); i++) {
                ExhibitionRankVO vo = list.get(i);
                // 排名要接着上一页继续排，不能每页都从 1 开始
                vo.setRank(baseRank + i + 1);

                int count = vo.getTicketCount() == null ? 0 : vo.getTicketCount();
                double ratio = denominator <= 0
                        ? 0D
                        : java.math.BigDecimal.valueOf(count * 100.0 / denominator)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
                vo.setVisitorRatio(ratio);
            }
        }
        return PageResult.of(total, dto.getPage(), dto.getLimit(), list);
    }

    // ==================================================================
    // 票务数据报表
    // ==================================================================

    /**
     * 票务数据表格
     * <p>
     * ⚠️ 口径关键点：**出售按支付日、核销按核销日**，两者可能不是同一天。
     * 所以分两次查（各自按自己的日期分组），再在 Service 层按「日期 + 展览」合并。
     * 若只按支付日分组，会把「今天卖的票」算成「今天核销的」，导致未核销数虚高。
     */
    @Override
    public PageResult<TicketStatVO> ticketStats(AdminStatsQueryDTO dto) {
        prepare(dto);

        long total = statsMapper.countTicketStats(dto);
        if (total == 0) {
            return PageResult.of(0, dto.getPage(), dto.getLimit(), new ArrayList<>());
        }

        List<TicketStatVO> list = statsMapper.listTicketStats(dto);
        fillVerifiedCount(list, dto);
        return PageResult.of(total, dto.getPage(), dto.getLimit(), list);
    }

    @Override
    public TicketStatsSummaryVO ticketStatsSummary(AdminStatsQueryDTO dto) {
        prepare(dto);
        LocalDate today = LocalDate.now();
        return statsMapper.getTicketStatsSummary(dto,
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    @Override
    public List<TicketStatVO> exportTicketStats(AdminStatsQueryDTO dto) {
        prepare(dto);
        // 导出不分页：把 pageSize 拉到足够大，复用同一套查询
        dto.setPage(1);
        dto.setPageSize(100000);
        List<TicketStatVO> list = statsMapper.listTicketStats(dto);
        fillVerifiedCount(list, dto);
        return list;
    }

    /**
     * 把「按核销日统计的核销数」合并进「按支付日统计的出售数」里
     * <p>
     * 合并键是「展览ID + 日期」。只有当某天既有出售又有核销时才对得上；
     * 只核销没出售的日期不会出现在表里（因为表格是按出售日为主键的）。
     */
    private void fillVerifiedCount(List<TicketStatVO> list, AdminStatsQueryDTO dto) {
        if (list.isEmpty()) {
            return;
        }

        List<TicketStatVO> verifiedList = statsMapper.listVerifiedByUseDate(dto);
        Map<String, Integer> verifiedMap = new HashMap<>();
        for (TicketStatVO v : verifiedList) {
            verifiedMap.put(key(v.getExhibitionId(), v.getStatDate()),
                    v.getVerifiedCount() == null ? 0 : v.getVerifiedCount());
        }

        for (TicketStatVO vo : list) {
            int verified = verifiedMap.getOrDefault(key(vo.getExhibitionId(), vo.getStatDate()), 0);
            int sold = vo.getSoldCount() == null ? 0 : vo.getSoldCount();
            vo.setVerifiedCount(verified);
            // 未核销 = 当日出售 - 当日核销；理论上不应为负，兜一下底
            vo.setUnverifiedCount(Math.max(sold - verified, 0));
        }
    }

    private String key(Long exhibitionId, LocalDate date) {
        return exhibitionId + "@" + date;
    }

    // ==================================================================
    // 私有工具
    // ==================================================================

    private void prepare(AdminStatsQueryDTO dto) {
        dto.setCity(trimToNull(dto.getCity()));
        dto.setKeyword(trimToNull(dto.getKeyword()));
        // 把 rangeType 解析成具体时间边界，XML 里直接用，避免 SQL 里写日期函数
        LocalDate[] range = resolveRange(dto);
        dto.setStartTime(range[0].atStartOfDay());
        // 右开区间：+1 天，保证结束日期当天的数据被包含在内
        dto.setEndTime(range[1].plusDays(1).atStartOfDay());
    }

    /**
     * 解析时间范围 → [开始日期, 结束日期]（都是闭区间，含首尾）
     * <p>
     * 前端只传 {@code rangeType}，具体日期由后端算，避免前后端时区/口径不一致。
     */
    private LocalDate[] resolveRange(AdminStatsQueryDTO dto) {
        LocalDate today = LocalDate.now();
        String rangeType = dto.getRangeType() == null ? "today" : dto.getRangeType().trim();

        switch (rangeType) {
            case "week":
                // 本周：周一到今天
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                return new LocalDate[]{monday, today};
            case "month":
                // 本月：1 号到今天
                return new LocalDate[]{today.withDayOfMonth(1), today};
            case "custom":
                if (dto.getStartDate() == null || dto.getEndDate() == null) {
                    throw new BaseException("自定义时间范围需要同时传 startDate 与 endDate");
                }
                if (dto.getEndDate().isBefore(dto.getStartDate())) {
                    throw new BaseException("结束日期不能早于开始日期");
                }
                return new LocalDate[]{dto.getStartDate(), dto.getEndDate()};
            case "today":
            default:
                return new LocalDate[]{today, today};
        }
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
