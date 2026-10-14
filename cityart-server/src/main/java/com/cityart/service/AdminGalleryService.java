package com.cityart.service;

import com.cityart.dto.AdminGalleryPageQueryDTO;
import com.cityart.dto.AdminGallerySaveDTO;
import com.cityart.result.PageResult;
import com.cityart.vo.AdminGalleryDetailVO;
import com.cityart.vo.AdminGalleryVO;

/**
 * 后台「展馆管理」业务
 */
public interface AdminGalleryService {

    /**
     * 展馆列表分页
     * <p>
     * ⚠️ 与 C 端的区别：后台**不筛** status，闭馆维护的馆也要能看到、能编辑
     */
    PageResult<AdminGalleryVO> pageQuery(AdminGalleryPageQueryDTO dto);

    /**
     * 展馆详情（编辑回填）
     */
    AdminGalleryDetailVO getDetailById(Long id);

    /**
     * 新增展馆
     * <p>
     * 后端会做两件运营看不到的事：自动生成展馆编号（如 NJ-G007）、
     * 由「闭馆日 + 开馆 + 闭馆」自动拼出 C 端展示的 openTime
     */
    Long create(AdminGallerySaveDTO dto);

    /**
     * 编辑展馆
     */
    void update(Long id, AdminGallerySaveDTO dto);

    /**
     * 删除展馆
     * <p>
     * ⚠️ 名下有展览 或 有过订单 都**不允许删除**（订单是财务数据，不能让场馆信息悬空）
     */
    void delete(Long id);
}
