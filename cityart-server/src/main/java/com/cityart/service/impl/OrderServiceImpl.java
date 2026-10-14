package com.cityart.service.impl;

import com.cityart.constant.MessageConstant;
import com.cityart.dto.OrderCancelDTO;
import com.cityart.dto.OrderCreateDTO;
import com.cityart.dto.OrderPageQueryDTO;
import com.cityart.dto.OrderPayDTO;
import com.cityart.dto.OrderRefundDTO;
import com.cityart.entity.ExhibitionCalendar;
import com.cityart.entity.ExhibitionOrder;
import com.cityart.entity.TicketType;
import com.cityart.enumeration.OrderStatusEnum;
import com.cityart.exception.BaseException;
import com.cityart.mapper.ExhibitionCalendarMapper;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.ExhibitionOrderMapper;
import com.cityart.mapper.TicketTypeMapper;
import com.cityart.result.PageResult;
import com.cityart.service.OrderService;
import com.cityart.utils.OrderNoUtil;
import com.cityart.vo.OrderCreateVO;
import com.cityart.vo.OrderDetailVO;
import com.cityart.vo.OrderPageItemVO;
import com.cityart.vo.PayParamVO;
import com.cityart.vo.PayResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    /** 待支付订单的超时时间：30 分钟 */
    private static final int PAY_TIMEOUT_MINUTES = 30;
    /** 票的有效期：观展日期 + 45 天 */
    private static final int TICKET_VALID_DAYS = 45;

    @Autowired
    private ExhibitionOrderMapper orderMapper;

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Autowired
    private ExhibitionCalendarMapper calendarMapper;

    @Autowired
    private ExhibitionMapper exhibitionMapper;

    /**
     * 下单：扣两个维度库存 + 插订单，必须包在一个事务里，
     * 任何一步失败都要回滚，否则库存会凭空少掉
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(Long userId, OrderCreateDTO dto) {
        // ① 校验票种存在且在售
        TicketType ticketType = ticketTypeMapper.getById(dto.getTicketTypeId());
        if (ticketType == null
                || ticketType.getStatus() == null || ticketType.getStatus() != 0
                || !ticketType.getExhibitionId().equals(dto.getExhibitionId())) {
            throw new BaseException(MessageConstant.TICKET_TYPE_NOT_FOUND);
        }

        // ② 校验日期在日历里且未售完
        ExhibitionCalendar calendar = calendarMapper.getByDate(dto.getExhibitionId(), dto.getVisitDate());
        if (calendar == null) {
            throw new BaseException(MessageConstant.VISIT_DATE_INVALID);
        }
        int dayRemaining = calendar.getTotalStock() - calendar.getSoldCount();
        if (calendar.getStatus() != null && calendar.getStatus() == 1 || dayRemaining <= 0) {
            throw new BaseException(MessageConstant.CALENDAR_SOLD_OUT);
        }

        // ③ 校验限购：已购（待支付/已支付/已完成） + 本次数量 ≤ 每人限购
        int limitPerUser = ticketType.getLimitPerUser() == null ? 0 : ticketType.getLimitPerUser();
        int bought = orderMapper.sumUserQuantity(userId, dto.getExhibitionId());
        if (limitPerUser > 0 && bought + dto.getQuantity() > limitPerUser) {
            throw new BaseException(String.format(MessageConstant.ORDER_QUANTITY_LIMIT, limitPerUser));
        }

        // 展览必须存在（顺便取美术馆ID存进订单）
        Long galleryId = exhibitionMapper.getGalleryIdById(dto.getExhibitionId());
        if (galleryId == null) {
            throw new BaseException(MessageConstant.EXHIBITION_NOT_FOUND);
        }

        // ④ 扣票种库存：条件更新，影响行数 0 说明库存不足
        int ticketRows = ticketTypeMapper.decreaseStock(ticketType.getId(), dto.getQuantity());
        if (ticketRows == 0) {
            throw new BaseException(MessageConstant.STOCK_NOT_ENOUGH);
        }

        // ⑤ 扣日历库存：条件更新，影响行数 0 说明当天已售完
        int calendarRows = calendarMapper.decreaseStock(dto.getExhibitionId(), dto.getVisitDate(), dto.getQuantity());
        if (calendarRows == 0) {
            throw new BaseException(MessageConstant.CALENDAR_SOLD_OUT);
        }

        // ⑥ 生成订单号并 INSERT（status = 1 待支付）
        BigDecimal totalAmount = ticketType.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
        LocalDateTime payExpireTime = LocalDateTime.now().plusMinutes(PAY_TIMEOUT_MINUTES);
        ExhibitionOrder order = ExhibitionOrder.builder()
                .orderNo(OrderNoUtil.generate())
                .userId(userId)
                .exhibitionId(dto.getExhibitionId())
                .galleryId(galleryId)
                .ticketTypeId(ticketType.getId())
                .ticketName(ticketType.getName())
                .price(ticketType.getPrice())
                .quantity(dto.getQuantity())
                .totalAmount(totalAmount)
                .status(OrderStatusEnum.WAIT_PAY.getValue())
                .visitDate(dto.getVisitDate())
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .payExpireTime(payExpireTime)
                .refundStatus(0)
                .createTime(LocalDateTime.now())
                .build();
        orderMapper.insert(order);
        log.info("下单成功，orderNo：{}，userId：{}，金额：{}", order.getOrderNo(), userId, totalAmount);

        // ⑦ 返回订单号 + 剩余秒数（前端倒计时直接用，不依赖客户端时间）
        return OrderCreateVO.builder()
                .orderNo(order.getOrderNo())
                .totalAmount(totalAmount)
                .status(OrderStatusEnum.WAIT_PAY.getValue())
                .payExpireTime(payExpireTime)
                .remainingSeconds((long) PAY_TIMEOUT_MINUTES * 60)
                .build();
    }

    @Override
    public OrderDetailVO getDetail(Long userId, String orderNo) {
        OrderDetailVO vo = orderMapper.getOrderDetail(orderNo, userId);
        if (vo == null) {
            // 不存在 → 抛「订单不存在」；是别人的订单 → 抛「无权操作该订单」
            getOwnOrder(orderNo, userId);
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }
        vo.setStatusText(OrderStatusEnum.textOf(vo.getStatus()));
        vo.setRemainingSeconds(remainingSeconds(vo.getStatus(), vo.getPayExpireTime()));
        return vo;
    }

    /**
     * 获取支付参数
     * <p>
     * ⚠️ 真实项目这里要调用微信/支付宝的「统一下单」接口拿 prepay_id 和签名，
     * 本项目没有商户号，所以返回的是**模拟支付参数**，前端流程可以完整走通。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayParamVO pay(Long userId, String orderNo, OrderPayDTO dto) {
        ExhibitionOrder order = getOwnOrder(orderNo, userId);

        if (!OrderStatusEnum.WAIT_PAY.getValue().equals(order.getStatus())) {
            throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 已超时：先把订单取消并释放库存，再告诉前端重新下单
        if (order.getPayExpireTime() != null && order.getPayExpireTime().isBefore(LocalDateTime.now())) {
            if (orderMapper.cancel(orderNo, userId, "支付超时自动取消") > 0) {
                releaseStock(order);
            }
            throw new BaseException(MessageConstant.ORDER_PAY_EXPIRED);
        }

        String prepayId = "wx" + orderNo + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return PayParamVO.builder()
                .orderNo(orderNo)
                .payType(dto.getPayType())
                .totalAmount(order.getTotalAmount())
                .qrCodeUrl("weixin://wxpay/bizpayurl?pr=" + orderNo)
                .prepayId(prepayId)
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .nonceStr(UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .packageValue("prepay_id=" + prepayId)
                .signType("RSA")
                .paySign("MOCK_SIGN_" + orderNo)
                .build();
    }

    /**
     * 支付结果回调（支付平台异步通知）
     * <p>
     * 真实项目第 ① 步要验签；本项目模拟环境跳过验签。
     * 第 ③ 步用「条件更新」做幂等：重复回调影响行数为 0，直接返回成功即可。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payNotify(Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        String orderNo = body == null ? null : String.valueOf(body.get("out_trade_no"));
        if (orderNo == null || "null".equals(orderNo)) {
            orderNo = body == null ? null : String.valueOf(body.get("orderNo"));
        }
        Object payTypeValue = body == null ? null : body.get("pay_type");
        int payType = payTypeValue == null ? 1 : Integer.parseInt(String.valueOf(payTypeValue));

        ExhibitionOrder order = orderNo == null ? null : orderMapper.getByOrderNo(orderNo);
        if (order == null) {
            log.error("支付回调：订单不存在，orderNo：{}", orderNo);
            result.put("code", "FAIL");
            result.put("message", "订单不存在");
            return result;
        }

        // 一单一码 + 有效期（观展日期 + 45 天）
        String qrCode = "/static/mock/qr/" + orderNo + ".png";
        LocalDateTime expireTime = order.getVisitDate() == null
                ? LocalDateTime.now().plusDays(TICKET_VALID_DAYS)
                : order.getVisitDate().plusDays(TICKET_VALID_DAYS).atStartOfDay();

        int rows = orderMapper.markPaid(orderNo, payType, qrCode, expireTime);
        if (rows == 0) {
            // 已经处理过了（重复回调），幂等返回成功
            log.info("支付回调重复到达，orderNo：{}，当前状态：{}", orderNo, order.getStatus());
        }

        result.put("code", "SUCCESS");
        result.put("message", "成功");
        return result;
    }

    @Override
    public PayResultVO payResult(Long userId, String orderNo) {
        ExhibitionOrder order = getOwnOrder(orderNo, userId);
        return PayResultVO.builder()
                .orderNo(orderNo)
                .status(order.getStatus())
                .statusText(OrderStatusEnum.textOf(order.getStatus()))
                .paid(OrderStatusEnum.PAID.getValue().equals(order.getStatus()))
                .payTime(order.getPayTime())
                .qrCode(order.getQrCode())
                .expireTime(order.getExpireTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, String orderNo, OrderCancelDTO dto) {
        ExhibitionOrder order = getOwnOrder(orderNo, userId);

        // 条件更新：只有「待支付」能被取消，重复点取消影响行数为 0
        int rows = orderMapper.cancel(orderNo, userId, dto == null ? null : dto.getCancelReason());
        if (rows == 0) {
            throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 同时释放两个维度的库存
        releaseStock(order);
        log.info("订单已取消，orderNo：{}", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, String orderNo, OrderRefundDTO dto) {
        ExhibitionOrder order = getOwnOrder(orderNo, userId);

        // 只有已支付、未核销、未过展期、未申请过退款的订单能退
        if (!OrderStatusEnum.PAID.getValue().equals(order.getStatus())) {
            throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (order.getUseTime() != null) {
            throw new BaseException(MessageConstant.REFUND_ALREADY_USED);
        }
        OrderDetailVO detail = orderMapper.getOrderDetail(orderNo, userId);
        if (detail != null && detail.getExhibitionEndDate() != null
                && detail.getExhibitionEndDate().isBefore(LocalDate.now())) {
            throw new BaseException(MessageConstant.REFUND_EXHIBITION_ENDED);
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() != 0) {
            throw new BaseException(MessageConstant.REFUND_PROCESSING);
        }

        int rows = orderMapper.refund(orderNo, userId);
        if (rows == 0) {
            throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
        }
        releaseStock(order);
        // 表里没有 refund_reason 字段，原因只记日志（要展示的话得给表加字段）
        log.info("退款成功，orderNo：{}，原因：{}", orderNo, dto == null ? null : dto.getRefundReason());
    }

    @Override
    public PageResult<OrderPageItemVO> pageOrders(Long userId, OrderPageQueryDTO dto) {
        long total = orderMapper.countOrderPage(userId, dto);

        List<OrderPageItemVO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            list = orderMapper.listOrderPage(userId, dto);
            list.forEach(this::fillOrderItem);
        }
        return PageResult.of(total, dto.getPage(), dto.getPageSize(), list);
    }

    /**
     * 列表卡片上的派生字段：状态中文、倒计时、三个按钮能不能点
     */
    private void fillOrderItem(OrderPageItemVO vo) {
        LocalDateTime now = LocalDateTime.now();
        vo.setStatusText(OrderStatusEnum.textOf(vo.getStatus()));

        // 只有待支付才给倒计时
        vo.setRemainingSeconds(remainingSeconds(vo.getStatus(), vo.getPayExpireTime()));

        boolean waitPay = OrderStatusEnum.WAIT_PAY.getValue().equals(vo.getStatus());
        vo.setCanPay(waitPay && vo.getPayExpireTime() != null && vo.getPayExpireTime().isAfter(now));

        boolean paid = OrderStatusEnum.PAID.getValue().equals(vo.getStatus());
        vo.setHasTicket(paid);

        boolean notUsed = vo.getUseTime() == null;
        boolean notEnded = vo.getExhibitionEndDate() == null || !vo.getExhibitionEndDate().isBefore(LocalDate.now());
        vo.setCanRefund(paid && notUsed && notEnded);
    }

    /**
     * 剩余支付秒数：只有「待支付」才有意义，其他状态返回 null
     */
    private Long remainingSeconds(Integer status, LocalDateTime payExpireTime) {
        if (!OrderStatusEnum.WAIT_PAY.getValue().equals(status) || payExpireTime == null) {
            return null;
        }
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), payExpireTime);
        return Math.max(seconds, 0);
    }

    /**
     * 按订单号取订单，并校验归属（防止拿别人的订单号操作别人的订单）
     */
    private ExhibitionOrder getOwnOrder(String orderNo, Long userId) {
        ExhibitionOrder order = orderMapper.getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BaseException(MessageConstant.ORDER_NOT_BELONG_TO_USER);
        }
        return order;
    }

    /**
     * 释放两个维度的库存（取消 / 退款都要调）
     */
    private void releaseStock(ExhibitionOrder order) {
        ticketTypeMapper.increaseStock(order.getTicketTypeId(), order.getQuantity());
        calendarMapper.increaseStock(order.getExhibitionId(), order.getVisitDate(), order.getQuantity());
    }
}
