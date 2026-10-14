package com.cityart.controller.admin;

import com.cityart.dto.AdminStatsQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.AdminStatsService;
import com.cityart.utils.ExcelUtil;
import com.cityart.vo.DashboardOverviewVO;
import com.cityart.vo.ExhibitionRankVO;
import com.cityart.vo.TicketStatVO;
import com.cityart.vo.TicketStatsSummaryVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * B 端「数据看板 + 票务数据报表」接口
 * <pre>
 * GET /api/admin/dashboard/overview            看板顶部三张卡片
 * GET /api/admin/dashboard/exhibition-rank     展览排行（含客流占比）
 * GET /api/admin/stats/tickets                 票务数据表格
 * GET /api/admin/stats/tickets/summary         票务数据底部三张卡片
 * GET /api/admin/stats/tickets/export          导出 Excel（返回文件流，非 Result）
 * </pre>
 * 时间范围统一用 rangeType（today/week/month/custom）传，由后端算起止日期。
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "B端数据看板与统计接口")
@Slf4j
public class AdminStatsController {

    @Autowired
    private AdminStatsService adminStatsService;

    /**
     * 看板核心指标
     * <p>
     * 三个数字的口径：到馆人数按**核销时间**算今天；售票量与营业额按 rangeType 范围、**支付时间**算
     */
    @GetMapping("/dashboard/overview")
    @ApiOperation("看板核心指标")
    public Result<DashboardOverviewVO> overview(@Validated AdminStatsQueryDTO dto) {
        log.info("后台查询看板指标，rangeType：{}，city：{}，type：{}",
                dto.getRangeType(), dto.getCity(), dto.getType());
        return Result.success(adminStatsService.overview(dto));
    }

    /**
     * 展览排行（排名 + 累计售票数 + 客流占比）
     */
    @GetMapping("/dashboard/exhibition-rank")
    @ApiOperation("展览排行")
    public Result<PageResult<ExhibitionRankVO>> exhibitionRank(@Validated AdminStatsQueryDTO dto) {
        log.info("后台查询展览排行，rangeType：{}，city：{}，type：{}，page：{}/{}",
                dto.getRangeType(), dto.getCity(), dto.getType(), dto.getPage(), dto.getPageSize());
        return Result.success(adminStatsService.exhibitionRank(dto));
    }

    /**
     * 票务数据报表
     * <p>
     * 口径：出售按**支付日**、核销按**核销日**（两者可能不是同一天），Service 层合并
     */
    @GetMapping("/stats/tickets")
    @ApiOperation("票务数据报表")
    public Result<PageResult<TicketStatVO>> ticketStats(@Validated AdminStatsQueryDTO dto) {
        log.info("后台查询票务数据，rangeType：{}，city：{}，type：{}，keyword：{}",
                dto.getRangeType(), dto.getCity(), dto.getType(), dto.getKeyword());
        return Result.success(adminStatsService.ticketStats(dto));
    }

    /**
     * 票务数据底部三张卡片
     */
    @GetMapping("/stats/tickets/summary")
    @ApiOperation("票务数据汇总")
    public Result<TicketStatsSummaryVO> ticketStatsSummary(@Validated AdminStatsQueryDTO dto) {
        log.info("后台查询票务汇总，rangeType：{}，city：{}，type：{}",
                dto.getRangeType(), dto.getCity(), dto.getType());
        return Result.success(adminStatsService.ticketStatsSummary(dto));
    }

    /**
     * 导出票务数据（Excel）
     * <p>
     * ⚠️ 这个接口返回的是**文件流**，不是 Result 结构，前端要用 blob 方式接收。
     */
    @GetMapping("/stats/tickets/export")
    @ApiOperation("导出票务数据")
    public void exportTicketStats(@Validated AdminStatsQueryDTO dto, HttpServletResponse response) throws IOException {
        log.info("后台导出票务数据，rangeType：{}，city：{}，type：{}，keyword：{}",
                dto.getRangeType(), dto.getCity(), dto.getType(), dto.getKeyword());

        List<TicketStatVO> list = adminStatsService.exportTicketStats(dto);

        String[] headers = {"统计日期", "展览名称", "当日出售票数", "当日核销票数", "当日未核销票数", "单日营收"};
        List<String[]> rows = new ArrayList<>();
        for (TicketStatVO vo : list) {
            rows.add(new String[]{
                    vo.getStatDate() == null ? "" : vo.getStatDate().toString(),
                    vo.getExhibitionTitle(),
                    str(vo.getSoldCount()),
                    str(vo.getVerifiedCount()),
                    str(vo.getUnverifiedCount()),
                    money(vo.getDailyRevenue())
            });
        }
        byte[] bytes = ExcelUtil.write("票务数据", headers, rows);

        // 文件名带日期，避免多次导出互相覆盖
        String fileName = "票务数据_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // URLEncoder 编码文件名，否则中文文件名在部分浏览器会乱码
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8"));
        response.setContentLength(bytes.length);

        try (OutputStream out = response.getOutputStream()) {
            out.write(bytes);
            out.flush();
        }
        log.info("票务数据导出完成，共 {} 行", rows.size());
    }

    private String str(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    /** 金额统一保留两位小数，Excel 里看起来整齐 */
    private String money(BigDecimal value) {
        return value == null
                ? "0.00"
                : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
