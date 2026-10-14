package com.cityart.service.impl;

import com.cityart.constant.MessageConstant;
import com.cityart.dto.OrderVerifyDTO;
import com.cityart.dto.VerifyRecordPageQueryDTO;
import com.cityart.entity.ExhibitionOrder;
import com.cityart.exception.BaseException;
import com.cityart.mapper.ExhibitionOrderMapper;
import com.cityart.mapper.UserMapper;
import com.cityart.result.PageResult;
import com.cityart.service.AdminVerifyService;
import com.cityart.vo.OrderVerifyVO;
import com.cityart.vo.VerifyRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * B 端扫码核销实现
 */
@Service
@Slf4j
public class AdminVerifyServiceImpl implements AdminVerifyService {

    /** 核销方式文案 */
    private static final String TYPE_SCAN = "扫码";
    private static final String TYPE_MANUAL = "输入订单号";

    @Autowired
    private ExhibitionOrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 核销 —— 一个事务里做两件事：
     * 「订单改成已完成」与「用户观展次数 +1」必须同成功同失败，
     * 否则会出现「票核销了但个人中心观展次数没涨」。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVerifyVO verify(OrderVerifyDTO dto, Long adminId) {
        // 1. 取出要核销的订单号：扫码方式传的是二维码内容（当前就是订单号）
        String orderNo = resolveOrderNo(dto);

        ExhibitionOrder order = orderMapper.getByOrderNo(orderNo);
        if (order == null) {
            throw new BaseException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 2. 必须是「已支付」才能核销
        if (order.getStatus() == null || order.getStatus() != 2) {
            if (order.getUseTime() != null || Integer.valueOf(5).equals(order.getStatus())) {
                throw new BaseException(MessageConstant.VERIFY_ALREADY_USED);
            }
            if (Integer.valueOf(1).equals(order.getStatus())) {
                throw new BaseException(MessageConstant.VERIFY_NOT_PAID);
            }
            throw new BaseException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 3. 未到观展日期不能核销（原型观展须知：「请确认观展日期，闭馆日无法核销入场」）
        LocalDate today = LocalDate.now();
        if (order.getVisitDate() != null && order.getVisitDate().isAfter(today)) {
            throw new BaseException(MessageConstant.VERIFY_TOO_EARLY);
        }

        // 4. 电子票有效期过了不能核销
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BaseException(MessageConstant.VERIFY_EXPIRED);
        }

        // 5. 条件更新核销：只有 status=2 改得动，防两个核销员同时扫同一张票
        int rows = orderMapper.verify(orderNo, dto.getVerifyType(), adminId);
        if (rows == 0) {
            throw new BaseException(MessageConstant.VERIFY_ALREADY_USED);
        }

        // 6. 观展次数 +1（个人中心「观展次数」的数据源）
        if (order.getUserId() != null) {
            userMapper.increaseVisitCount(order.getUserId());
        }

        log.info("核销成功，orderNo：{}，方式：{}，操作员：{}", orderNo, dto.getVerifyType(), adminId);

        // 7. 组装回执，让核销员确认核销的是谁、几张
        OrderVerifyVO vo = orderMapper.getVerifyInfo(orderNo);
        if (vo == null) {
            // 极端情况：刚核销完订单被删。返回基本信息即可
            vo = OrderVerifyVO.builder().orderNo(orderNo).build();
        }
        vo.setUseTime(LocalDateTime.now());
        vo.setVerifyType(dto.getVerifyType());
        return vo;
    }

    @Override
    public PageResult<VerifyRecordVO> pageRecords(VerifyRecordPageQueryDTO dto) {
        dto.setCity(trimToNull(dto.getCity()));
        dto.setKeyword(trimToNull(dto.getKeyword()));

        long total = orderMapper.countVerifyRecords(dto);

        List<VerifyRecordVO> list;
        if (total == 0) {
            list = new ArrayList<>();
        } else {
            list = orderMapper.listVerifyRecords(dto);
            // 核销方式中文名由后端给，前端不硬编码
            list.forEach(vo -> vo.setVerifyTypeText(
                    Integer.valueOf(2).equals(vo.getVerifyType()) ? TYPE_MANUAL : TYPE_SCAN));
        }
        return PageResult.of(total, dto.getPage(), dto.getLimit(), list);
    }

    /**
     * 从两种核销方式里取出订单号
     * <p>
     * 扫码方式传的是二维码内容；当前二维码内容就是订单号，
     * 若以后二维码改成带前缀（如 CA:20261001000001），在这里解析即可，调用方不用改。
     */
    private String resolveOrderNo(OrderVerifyDTO dto) {
        String raw = Integer.valueOf(2).equals(dto.getVerifyType())
                ? dto.getOrderNo()
                : dto.getQrCode();

        String orderNo = trimToNull(raw);
        if (orderNo == null) {
            throw new BaseException(MessageConstant.VERIFY_PARAM_MISSING);
        }
        // 兼容「带前缀的二维码内容」
        int idx = orderNo.lastIndexOf(':');
        return idx >= 0 ? orderNo.substring(idx + 1).trim() : orderNo;
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
