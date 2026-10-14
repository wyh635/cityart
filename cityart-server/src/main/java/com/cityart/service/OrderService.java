package com.cityart.service;

import com.cityart.dto.OrderCancelDTO;
import com.cityart.dto.OrderCreateDTO;
import com.cityart.dto.OrderPageQueryDTO;
import com.cityart.dto.OrderPayDTO;
import com.cityart.dto.OrderRefundDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.OrderCreateVO;
import com.cityart.vo.OrderDetailVO;
import com.cityart.vo.OrderPageItemVO;
import com.cityart.vo.PayParamVO;
import com.cityart.vo.PayResultVO;

import java.util.Map;

/**
 * 订单业务（下单 / 支付 / 取消 / 退款 / 我的订单）
 */
public interface OrderService {

    /** 创建订单（锁两个维度库存） */
    OrderCreateVO create(Long userId, OrderCreateDTO dto);

    /** 订单详情（支付页） */
    OrderDetailVO getDetail(Long userId, String orderNo);

    /** 获取支付参数（去支付） */
    PayParamVO pay(Long userId, String orderNo, OrderPayDTO dto);

    /** 支付结果回调（支付平台调用，不套 Result） */
    Map<String, Object> payNotify(Map<String, Object> body);

    /** 主动查询支付结果（前端轮询） */
    PayResultVO payResult(Long userId, String orderNo);

    /** 取消订单（待支付） */
    void cancel(Long userId, String orderNo, OrderCancelDTO dto);

    /** 申请退款（已支付且未核销、未过展期） */
    void refund(Long userId, String orderNo, OrderRefundDTO dto);

    /** 我的订单分页 */
    PageResult<OrderPageItemVO> pageOrders(Long userId, OrderPageQueryDTO dto);
}
