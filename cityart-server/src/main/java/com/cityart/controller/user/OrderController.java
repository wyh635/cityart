package com.cityart.controller.user;

import com.cityart.context.BaseContext;
import com.cityart.dto.OrderCancelDTO;
import com.cityart.dto.OrderCreateDTO;
import com.cityart.dto.OrderPageQueryDTO;
import com.cityart.dto.OrderPayDTO;
import com.cityart.dto.OrderRefundDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.OrderService;
import com.cityart.vo.OrderCreateVO;
import com.cityart.vo.OrderDetailVO;
import com.cityart.vo.OrderPageItemVO;
import com.cityart.vo.PayParamVO;
import com.cityart.vo.PayResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * C 端订单接口
 * <p>
 * ⚠️ 只有支付回调（pay-notify）是免登录的，其余接口**都要带 token**；
 * userId 一律从 token 取（BaseContext），**绝不接受前端传 userId**，否则能操作别人的订单。
 */
@RestController
@RequestMapping("/api/app/orders")
@Api(tags = "C端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单（购票页「确认下单」）
     */
    @PostMapping
    @ApiOperation("创建订单")
    public Result<OrderCreateVO> create(@Validated @RequestBody OrderCreateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("创建订单，userId：{}，exhibitionId：{}，票种：{}，数量：{}",
                userId, dto.getExhibitionId(), dto.getTicketTypeId(), dto.getQuantity());
        return Result.success("下单成功", orderService.create(userId, dto));
    }

    /**
     * 我的订单分页（顶部 5 个 tab）
     */
    @GetMapping("/page")
    @ApiOperation("我的订单分页")
    public Result<PageResult<OrderPageItemVO>> page(@Validated OrderPageQueryDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("我的订单，userId：{}，status：{}，page：{}/{}", userId, dto.getStatus(), dto.getPage(), dto.getPageSize());
        return Result.success(orderService.pageOrders(userId, dto));
    }

    /**
     * 订单详情（支付页）
     */
    @GetMapping("/{orderNo}")
    @ApiOperation("订单详情")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        Long userId = BaseContext.getCurrentId();
        log.info("订单详情，userId：{}，orderNo：{}", userId, orderNo);
        return Result.success(orderService.getDetail(userId, orderNo));
    }

    /**
     * 获取支付参数（去支付）；真实环境是调微信下单接口，本项目返回模拟参数
     */
    @PostMapping("/{orderNo}/pay")
    @ApiOperation("获取支付参数")
    public Result<PayParamVO> pay(@PathVariable String orderNo, @Validated @RequestBody OrderPayDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("去支付，userId：{}，orderNo：{}，payType：{}", userId, orderNo, dto.getPayType());
        return Result.success(orderService.pay(userId, orderNo, dto));
    }

    /**
     * 主动查询支付结果（前端轮询）
     */
    @GetMapping("/{orderNo}/pay-result")
    @ApiOperation("查询支付结果")
    public Result<PayResultVO> payResult(@PathVariable String orderNo) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(orderService.payResult(userId, orderNo));
    }

    /**
     * 取消订单（待支付）
     */
    @PutMapping("/{orderNo}/cancel")
    @ApiOperation("取消订单")
    public Result<Void> cancel(@PathVariable String orderNo, @RequestBody(required = false) OrderCancelDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("取消订单，userId：{}，orderNo：{}", userId, orderNo);
        orderService.cancel(userId, orderNo, dto);
        return Result.successMsg("已取消");
    }

    /**
     * 申请退款（已支付且未核销、未过展期）
     */
    @PostMapping("/{orderNo}/refund")
    @ApiOperation("申请退款")
    public Result<Void> refund(@PathVariable String orderNo, @RequestBody(required = false) OrderRefundDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("申请退款，userId：{}，orderNo：{}", userId, orderNo);
        orderService.refund(userId, orderNo, dto);
        return Result.successMsg("退款申请已提交");
    }

    /**
     * 支付结果回调（支付平台调用）
     * <p>
     * ⚠️ 本接口**不套 Result**，按支付平台要求返回 {"code":"SUCCESS","message":"成功"}
     */
    @PostMapping("/pay-notify")
    @ApiOperation("支付结果回调")
    public Map<String, Object> payNotify(@RequestBody(required = false) Map<String, Object> body) {
        log.info("收到支付回调：{}", body);
        return orderService.payNotify(body);
    }
}
