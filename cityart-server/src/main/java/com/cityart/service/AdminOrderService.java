package com.cityart.service;

import com.cityart.dto.AdminOrderPageQueryDTO;
import com.cityart.dto.AdminOrderRefundDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.AdminOrderVO;

/**
 * B 端「订单管理」业务
 */
public interface AdminOrderService {

    /**
     * 订单列表分页（后台查所有用户的订单）
     */
    PageResult<AdminOrderVO> pageQuery(AdminOrderPageQueryDTO dto);

    /**
     * 退款审核
     * <p>
     * 同意退款时（同一事务）：改状态为已退款 → 回滚两个维度的库存 → 作废电子票
     *
     * @param adminId 操作管理员ID（记日志用）
     */
    void refund(String orderNo, AdminOrderRefundDTO dto, Long adminId);
}
