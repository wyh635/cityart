package com.cityart.controller.admin;

import com.cityart.context.AdminContext;
import com.cityart.dto.AdminOrderPageQueryDTO;
import com.cityart.dto.AdminOrderRefundDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.AdminOrderService;
import com.cityart.vo.AdminOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端「订单管理」接口
 * <pre>
 * GET /api/admin/orders/page                   订单列表（后台查所有用户订单）
 * PUT /api/admin/orders/{orderNo}/refund       退款审核
 * </pre>
 * 注意：核销接口在 {@code AdminVerifyController}（/api/admin/orders/verify）
 */
@RestController
@RequestMapping("/api/admin/orders")
@Api(tags = "B端订单管理接口")
@Slf4j
public class AdminOrderController {

    @Autowired
    private AdminOrderService adminOrderService;

    /**
     * 订单列表分页
     * <p>
     * 筛选：订单号（精确）/ 手机号 / 购票人 / 展览 / 城市 / 状态 / 下单日期范围
     */
    @GetMapping("/page")
    @ApiOperation("订单列表分页")
    public Result<PageResult<AdminOrderVO>> page(@Validated AdminOrderPageQueryDTO dto) {
        log.info("后台查询订单列表，orderNo：{}，phone：{}，status：{}，page：{}/{}",
                dto.getOrderNo(), dto.getPhone(), dto.getStatus(), dto.getPage(), dto.getPageSize());
        return Result.success(adminOrderService.pageQuery(dto));
    }

    /**
     * 退款审核
     * <p>
     * 同意后（同一事务）：改状态为已退款 → 回滚票种与日历两个维度的库存 → 作废电子票
     */
    @PutMapping("/{orderNo}/refund")
    @ApiOperation("退款审核")
    public Result<Void> refund(@PathVariable String orderNo,
                               @Validated @RequestBody AdminOrderRefundDTO dto) {
        Long adminId = AdminContext.getCurrentId();
        log.info("后台退款审核，orderNo：{}，approve：{}，adminId：{}", orderNo, dto.getApprove(), adminId);
        adminOrderService.refund(orderNo, dto, adminId);
        return Result.successMsg(Boolean.FALSE.equals(dto.getApprove()) ? "已驳回" : "退款已处理");
    }
}
