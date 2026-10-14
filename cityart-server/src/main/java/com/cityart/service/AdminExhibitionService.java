package com.cityart.service;

import com.cityart.dto.AdminExhibitionPageQueryDTO;
import com.cityart.dto.AdminExhibitionSaveDTO;
import com.cityart.dto.AdminExhibitionStatusDTO;
import com.cityart.dto.AdminExhibitionStockDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.AdminExhibitionCreateVO;
import com.cityart.vo.AdminExhibitionDetailVO;
import com.cityart.vo.AdminExhibitionVO;

/**
 * 后台「展览管理」业务
 */
public interface AdminExhibitionService {

    /**
     * 展览列表分页（原型：展览ID / 展览名称 / 所属场馆 / 展期 / 票价 / 当日库存）
     * <p>
     * 「当日库存」与「已售完」由 Service 批量计算回填
     */
    PageResult<AdminExhibitionVO> pageQuery(AdminExhibitionPageQueryDTO dto);

    /**
     * 展览详情（编辑回填，含 4 个票种与已生成的日历天数）
     */
    AdminExhibitionDetailVO getDetailById(Long id);

    /**
     * 新增展览
     * <p>
     * 一个事务里做四件事：插展览 → 生成展览编号 → 插票种 → 按展期批量生成日历库存
     */
    AdminExhibitionCreateVO create(AdminExhibitionSaveDTO dto);

    /**
     * 编辑展览
     * <p>
     * 展期缩短时：未来且未售出的日历行删掉，已售出的置为停售
     */
    void update(Long id, AdminExhibitionSaveDTO dto);

    /**
     * 删除展览
     * <p>
     * ⚠️ 有有效订单的展览**不允许删除**（订单是财务数据），提示改为下架
     */
    void delete(Long id);

    /**
     * 上下架 / 热门开关 / 排序
     */
    void updateStatus(Long id, AdminExhibitionStatusDTO dto);

    /**
     * 更新库存：改每日票数上限，或精确调整某几天的库存
     */
    void updateStock(Long id, AdminExhibitionStockDTO dto);
}
