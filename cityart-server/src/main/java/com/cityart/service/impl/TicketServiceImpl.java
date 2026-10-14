package com.cityart.service.impl;

import com.cityart.dto.RecordPageQueryDTO;
import com.cityart.dto.TicketPageQueryDTO;
import com.cityart.mapper.ExhibitionCalendarMapper;
import com.cityart.mapper.ExhibitionOrderMapper;
import com.cityart.mapper.TicketTypeMapper;
import com.cityart.service.TicketService;
import com.cityart.utils.DateTextUtil;
import com.cityart.vo.CalendarVO;
import com.cityart.vo.RecordPageVO;
import com.cityart.vo.RecordVO;
import com.cityart.vo.TicketPageVO;
import com.cityart.vo.TicketTypeVO;
import com.cityart.vo.TicketVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class TicketServiceImpl implements TicketService {

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Autowired
    private ExhibitionCalendarMapper calendarMapper;

    @Autowired
    private ExhibitionOrderMapper orderMapper;

    @Override
    public List<TicketTypeVO> listTicketTypes(Long exhibitionId) {
        return ticketTypeMapper.listByExhibitionId(exhibitionId);
    }

    @Override
    public List<CalendarVO> listCalendar(Long exhibitionId) {
        List<CalendarVO> list = calendarMapper.listByExhibitionId(exhibitionId);
        // 「周四」这种中文星期由后端算好，前端不拼
        list.forEach(vo -> {
            vo.setWeekDay(DateTextUtil.weekDayText(vo.getDate()));
            if (vo.getRemaining() == null) {
                vo.setRemaining(0);
            }
            if (vo.getSoldOut() == null) {
                vo.setSoldOut(Boolean.FALSE);
            }
        });
        return list;
    }

    @Override
    public TicketPageVO pageTickets(Long userId, TicketPageQueryDTO dto) {
        // 票夹的数据源就是「已支付订单」，所以总数 = 有效票数
        int validCount = orderMapper.countValidTickets(userId);

        List<TicketVO> list;
        if (validCount == 0) {
            list = Collections.emptyList();
        } else {
            list = orderMapper.listTickets(userId, dto);
            list.forEach(this::fillTicket);
        }

        return TicketPageVO.of(validCount, dto.getPage(), dto.getLimit(), list, validCount);
    }

    @Override
    public RecordPageVO pageRecords(Long userId, RecordPageQueryDTO dto) {
        long total = orderMapper.countRecords(userId, dto);

        List<RecordVO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            list = orderMapper.listRecords(userId, dto);
            // 月份中文由后端拼：2026-09 → 2026年9月
            list.forEach(vo -> vo.setMonthText(DateTextUtil.monthText(vo.getMonthGroup())));
        }

        int totalVisitCount = orderMapper.countVisited(userId);
        return RecordPageVO.of(total, dto.getPage(), dto.getLimit(), list, totalVisitCount);
    }

    /**
     * 补两个字段：还有几天过期、状态中文
     */
    private void fillTicket(TicketVO vo) {
        if (vo.getExpireTime() != null) {
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), vo.getExpireTime());
            vo.setRemainDays(Math.max(days, 0));
        } else {
            vo.setRemainDays(0L);
        }
        vo.setStatusText("有效");
    }
}
