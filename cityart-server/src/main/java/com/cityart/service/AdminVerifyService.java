package com.cityart.service;

import com.cityart.dto.OrderVerifyDTO;
import com.cityart.dto.VerifyRecordPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.OrderVerifyVO;
import com.cityart.vo.VerifyRecordVO;

/**
 * B 端「扫码核销」业务
 */
public interface AdminVerifyService {

    /**
     * 核销电子票（扫码 或 输入订单号）
     * <p>
     * 校验顺序：订单存在 → 已支付 → 未核销 → 已到观展日期 → 未过期 → 条件更新 → 观展次数 +1
     *
     * @param adminId 从 AdminContext 取，记入 verify_admin_id
     */
    OrderVerifyVO verify(OrderVerifyDTO dto, Long adminId);

    /**
     * 核销记录分页（数据源是已核销订单，不需要单独的核销记录表）
     */
    PageResult<VerifyRecordVO> pageRecords(VerifyRecordPageQueryDTO dto);
}
