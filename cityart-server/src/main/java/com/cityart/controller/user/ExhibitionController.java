package com.cityart.controller.user;

import com.cityart.annotation.OptionalLogin;
import com.cityart.context.BaseContext;
import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.dto.ExhibitionWorkPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.ExhibitionService;
import com.cityart.service.TicketService;
import com.cityart.vo.CalendarVO;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.ExhibitionPageVO;
import com.cityart.vo.ExhibitionWorkVO;
import com.cityart.vo.TicketTypeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端展览接口（全部免登录）
 * GET /api/app/exhibitions/page          全部展览分页列表
 * GET /api/app/exhibitions/{id}          展览详情
 * GET /api/app/exhibitions/{id}/works    展览作品集（分页）
 */
@RestController
@RequestMapping("/api/app/exhibitions")
@Api(tags = "C端展览接口")
@Slf4j
public class ExhibitionController {

    @Autowired
    private ExhibitionService exhibitionService;

    @Autowired
    private TicketService ticketService;

    /**
     * 全部展览分页列表
     * 「正在展出 / 即将展出 / 往期展出」三个区块共用本接口，用 status 区分：
     * status=current&pageSize=2 → 正在展出区块；status=future / past 同理
     * 参数校验注解写在 ExhibitionPageQueryDTO 的字段上，@Validated 让它生效
     */
    @GetMapping("/page")
    @ApiOperation("全部展览分页列表")
    public Result<PageResult<ExhibitionPageVO>> page(@Validated ExhibitionPageQueryDTO dto) {
        log.info("查询全部展览，status：{}，type：{}，city：{}，keyword：{}，sort：{}，page：{}/{}",
                dto.getStatus(), dto.getType(), dto.getCity(), dto.getKeyword(),
                dto.getSort(), dto.getPage(), dto.getPageSize());
        return Result.success(exhibitionService.pageQuery(dto));
    }

    /**
     * 展览详情（头部信息 + 展览简介 + 展览实景 + 票务信息）
     * <p>
     * 可选登录：游客可看，带 token 时 isFavorited 返回当前用户的真实收藏状态
     * 查不到时返回 {"code":0,"msg":"展览不存在"}
     */
    @GetMapping("/{id}")
    @OptionalLogin
    @ApiOperation("展览详情")
    public Result<ExhibitionDetailVO> detail(@PathVariable Long id) {
        log.info("查询展览详情，id：{}，userId：{}", id, BaseContext.getCurrentId());
        return Result.success(exhibitionService.getDetailById(id));
    }

    /**
     * 展览作品集（详情页「作品集 / COLLECTION」区块，独立分页，默认每页 3 件）
     * 详情接口只返回 worksCount，前端据此决定要不要调本接口
     */
    @GetMapping("/{id}/works")
    @ApiOperation("展览作品集分页列表")
    public Result<PageResult<ExhibitionWorkVO>> works(@PathVariable Long id,
                                                     @Validated ExhibitionWorkPageQueryDTO dto) {
        log.info("查询展览作品集，exhibitionId：{}，page：{}/{}", id, dto.getPage(), dto.getPageSize());
        return Result.success(exhibitionService.pageWorks(id, dto));
    }

    /**
     * 展览的票种列表（购票页「选择票种 TYPE」，免登录）
     */
    @GetMapping("/{id}/ticket-types")
    @ApiOperation("展览票种列表")
    public Result<List<TicketTypeVO>> ticketTypes(@PathVariable Long id) {
        log.info("查询票种列表，exhibitionId：{}", id);
        return Result.success(ticketService.listTicketTypes(id));
    }

    /**
     * 展览的可预约观展日期（购票页「选择观展日期 DATE」，含「已售完」，免登录）
     * <p>
     * 只返回日历表里有记录的日期 —— 这就是「只放 5 天票」的实现
     */
    @GetMapping("/{id}/calendar")
    @ApiOperation("展览观展日历")
    public Result<List<CalendarVO>> calendar(@PathVariable Long id) {
        log.info("查询观展日历，exhibitionId：{}", id);
        return Result.success(ticketService.listCalendar(id));
    }
}
