package com.cityart.controller.admin;

import com.cityart.context.AdminContext;
import com.cityart.dto.OrderVerifyDTO;
import com.cityart.dto.VerifyRecordPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.AdminVerifyService;
import com.cityart.vo.OrderVerifyVO;
import com.cityart.vo.VerifyRecordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端「扫码核销」接口
 * <pre>
 * POST /api/admin/orders/verify          核销电子票（扫码 或 输入订单号）
 * GET  /api/admin/verify-records/page    核销记录
 * </pre>
 * 核销操作员从 AdminContext 取，不由前端传（否则可以伪造核销人）
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "B端扫码核销接口")
@Slf4j
public class AdminVerifyController {

    @Autowired
    private AdminVerifyService adminVerifyService;

    /**
     * 核销电子票
     * <p>
     * verifyType=1 传 qrCode（扫码）；verifyType=2 传 orderNo（输入订单号）
     */
    @PostMapping("/orders/verify")
    @ApiOperation("核销电子票")
    public Result<OrderVerifyVO> verify(@Validated @RequestBody OrderVerifyDTO dto) {
        Long adminId = AdminContext.getCurrentId();
        log.info("扫码核销，方式：{}，adminId：{}", dto.getVerifyType(), adminId);
        return Result.success("核销成功", adminVerifyService.verify(dto, adminId));
    }

    /**
     * 核销记录分页
     * <p>
     * 数据源是已核销订单（use_time 不为空），不查单独的核销记录表
     */
    @GetMapping("/verify-records/page")
    @ApiOperation("核销记录分页")
    public Result<PageResult<VerifyRecordVO>> pageRecords(@Validated VerifyRecordPageQueryDTO dto) {
        log.info("查询核销记录，city：{}，type：{}，keyword：{}，page：{}/{}",
                dto.getCity(), dto.getType(), dto.getKeyword(), dto.getPage(), dto.getPageSize());
        return Result.success(adminVerifyService.pageRecords(dto));
    }
}
