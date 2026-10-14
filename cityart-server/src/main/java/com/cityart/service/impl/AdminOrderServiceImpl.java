package com.cityart.service.impl;

import com.cityart.constant.MessageConstant;
import com.cityart.dto.AdminOrderPageQueryDTO;
import com.cityart.dto.AdminOrderRefundDTO;
import com.cityart.entity.ExhibitionOrder;
import com.cityart.enumeration.OrderStatusEnum;
import com.cityart.exception.BaseException;
import com.cityart.mapper.ExhibitionCalendarMapper;
import com.cityart.mapper.ExhibitionOrderMapper;
import com.cityart.mapper.TicketTypeMapper;
import com.cityart.result.PageResult;
import com.cityart.service.AdminOrderService;
import com.cityart.utils.PhoneUtil;
import com.cityart.vo.AdminOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * B 端「订单管理」实现
 */
@Service
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    @Autowired
    private ExhibitionOrderMapper orderMapper;

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Autowired
    private ExhibitionCalendarMapper calendarMapper;

    @Override
    public PageResult<AdminOrderVO> pageQuery(AdminOrderPageQueryDTO dto) {
        dto.setOrderNo(trimToNull(dto.getOrderNo()));
        dto.setPhone(trimToNull(dto.getPhone()));
        dto.setContactName(trimToNull(dto.getContactName()));
        dto.setCity(trimToNull(dto.getCity()));

        long total = orderMapper.countAdminOrderPage(dto);

        List<AdminOrderVO> list;
        if (total == 0) {
            list = new ArrayList<>();
        } else {
            list = orderMapper.listAdminOrderPage(dto);
            list.forEach(vo -> {
                // 状态中文由后端给，前端不硬编码
                vo.setStatusText(OrderStatusEnum.textOf(vo.getStatus()));
                // 手机号脱敏：后台列表不需要看完整号码
                vo.setContactPhone(PhoneUtil.mask(vo.getContactPhone()));
            });
        }
        return PageResult.of(total, dto.getPage(), dto.getLimit(), list);
    }

    /**
     * 退款 —— 一个事务里做三件事：
     * ① 订单状态改为已退款 + 作废电子票
     * ② 回滚票种库存
     * ③ 回滚日历库存
     * <p>
     * 少了任何一步都会造成「钱退了但票还能用」或「库存对不上」。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, AdminOrderRefundDTO dto, Long adminId) {
        ExhibitionOrder order = orderMapper.getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 驳回：不改状态，只记日志（用户端仍显示「已支付」）
        if (Boolean.FALSE.equals(dto.getApprove())) {
            log.info("后台驳回退款申请，orderNo：{}，adminId：{}，备注：{}",
                    orderNo, adminId, dto.getRemark());
            return;
        }

        // 只有「已支付」能退；已核销的订单 status 已是 5，条件更新会失败
        if (!OrderStatusEnum.PAID.getValue().equals(order.getStatus())) {
            if (order.getUseTime() != null) {
                throw new BaseException(MessageConstant.REFUND_ALREADY_USED);
            }
            throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 条件更新：status = 2 才改得动，防两个管理员同时点退款
        int rows = orderMapper.refundByAdmin(orderNo);
        if (rows == 0) {
            throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 回滚两个维度的库存（下单时扣了两处，退款必须两处都还）
        if (order.getTicketTypeId() != null) {
            ticketTypeMapper.increaseStock(order.getTicketTypeId(), order.getQuantity());
        }
        if (order.getExhibitionId() != null && order.getVisitDate() != null) {
            calendarMapper.increaseStock(order.getExhibitionId(), order.getVisitDate(), order.getQuantity());
        }

        log.info("后台退款成功，orderNo：{}，金额：{}，adminId：{}，备注：{}",
                orderNo, order.getTotalAmount(), adminId, dto.getRemark());
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
