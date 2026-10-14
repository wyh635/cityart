package com.cityart.service;

import com.cityart.dto.RecordPageQueryDTO;
import com.cityart.dto.TicketPageQueryDTO;
import com.cityart.vo.CalendarVO;
import com.cityart.vo.RecordPageVO;
import com.cityart.vo.TicketPageVO;
import com.cityart.vo.TicketTypeVO;

import java.util.List;

/**
 * 票务查询业务（票种 / 观展日历 / 票夹 / 看展记录）
 */
public interface TicketService {

    /** 展览的票种列表（免登录） */
    List<TicketTypeVO> listTicketTypes(Long exhibitionId);

    /** 展览的可预约日期（免登录） */
    List<CalendarVO> listCalendar(Long exhibitionId);

    /** 票夹分页（登录，数据源是已支付订单） */
    TicketPageVO pageTickets(Long userId, TicketPageQueryDTO dto);

    /** 看展记录分页（登录，数据源是已核销订单） */
    RecordPageVO pageRecords(Long userId, RecordPageQueryDTO dto);
}
