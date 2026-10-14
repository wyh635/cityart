package com.cityart.controller.user;

import com.cityart.context.BaseContext;
import com.cityart.dto.TicketPageQueryDTO;
import com.cityart.result.Result;
import com.cityart.service.TicketService;
import com.cityart.vo.TicketPageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端票夹接口（登录后访问）
 * GET /api/app/tickets/page
 */
@RestController
@RequestMapping("/api/app/tickets")
@Api(tags = "C端票夹接口")
@Slf4j
public class TicketController {

    @Autowired
    private TicketService ticketService;

    /**
     * 票夹分页：数据源是「已支付订单」，一单一码
     * 原型一次显示一张卡，所以 pageSize 默认 1
     */
    @GetMapping("/page")
    @ApiOperation("票夹分页")
    public Result<TicketPageVO> page(@Validated TicketPageQueryDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("票夹，userId：{}，page：{}/{}", userId, dto.getPage(), dto.getPageSize());
        return Result.success(ticketService.pageTickets(userId, dto));
    }
}
