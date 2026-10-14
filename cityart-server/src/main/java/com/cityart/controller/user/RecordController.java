package com.cityart.controller.user;

import com.cityart.context.BaseContext;
import com.cityart.dto.RecordPageQueryDTO;
import com.cityart.result.Result;
import com.cityart.service.TicketService;
import com.cityart.vo.RecordPageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端看展记录接口（登录后访问）
 * GET /api/app/records/page
 */
@RestController
@RequestMapping("/api/app/records")
@Api(tags = "C端看展记录接口")
@Slf4j
public class RecordController {

    @Autowired
    private TicketService ticketService;

    /**
     * 看展记录分页：数据源是「已核销订单」（use_time 不为空），可按月筛选
     */
    @GetMapping("/page")
    @ApiOperation("看展记录分页")
    public Result<RecordPageVO> page(@Validated RecordPageQueryDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("看展记录，userId：{}，month：{}，page：{}/{}", userId, dto.getMonth(), dto.getPage(), dto.getPageSize());
        return Result.success(ticketService.pageRecords(userId, dto));
    }
}
