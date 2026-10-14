package com.cityart.controller.admin;

import com.cityart.dto.AdminExhibitionPageQueryDTO;
import com.cityart.dto.AdminExhibitionSaveDTO;
import com.cityart.dto.AdminExhibitionStatusDTO;
import com.cityart.dto.AdminExhibitionStockDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.AdminExhibitionService;
import com.cityart.vo.AdminExhibitionCreateVO;
import com.cityart.vo.AdminExhibitionDetailVO;
import com.cityart.vo.AdminExhibitionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端「展览管理」接口
 * <pre>
 * GET    /api/admin/exhibitions/page            展览列表（含当日库存 / 已售完）
 * GET    /api/admin/exhibitions/{id}            展览详情（编辑回填）
 * POST   /api/admin/exhibitions                 新增展览（自动生成编号 + 批量生成日历）
 * PUT    /api/admin/exhibitions/{id}            编辑展览
 * DELETE /api/admin/exhibitions/{id}            删除展览（有订单则拒绝）
 * PUT    /api/admin/exhibitions/{id}/status     上下架 / 热门开关
 * PUT    /api/admin/exhibitions/{id}/stock      更新库存
 * </pre>
 * 全部要求 B 端 token（拦截器已拦 /api/admin/**）
 */
@RestController
@RequestMapping("/api/admin/exhibitions")
@Api(tags = "B端展览管理接口")
@Slf4j
public class AdminExhibitionController {

    @Autowired
    private AdminExhibitionService adminExhibitionService;

    /**
     * 展览列表分页
     * <p>
     * 筛选：城市 / 库存状态 / 展览类型 / 搜索展览（名称或编号）
     */
    @GetMapping("/page")
    @ApiOperation("展览列表分页")
    public Result<PageResult<AdminExhibitionVO>> page(@Validated AdminExhibitionPageQueryDTO dto) {
        log.info("后台查询展览列表，city：{}，stockStatus：{}，type：{}，keyword：{}，page：{}/{}",
                dto.getCity(), dto.getStockStatus(), dto.getType(), dto.getKeyword(),
                dto.getPage(), dto.getPageSize());
        return Result.success(adminExhibitionService.pageQuery(dto));
    }

    /**
     * 展览详情（编辑时回填表单，含 4 个票种与已生成日历天数）
     */
    @GetMapping("/{id}")
    @ApiOperation("展览详情")
    public Result<AdminExhibitionDetailVO> detail(@PathVariable Long id) {
        log.info("后台查询展览详情，id：{}", id);
        return Result.success(adminExhibitionService.getDetailById(id));
    }

    /**
     * 新增展览
     * <p>
     * 后端会做两件运营看不到的事：
     * <ol>
     *     <li>按城市自动生成展览编号（如 NJ007）</li>
     *     <li>按展期把「每日票数上限」展开成 N 行日历库存</li>
     * </ol>
     */
    @PostMapping
    @ApiOperation("新增展览")
    public Result<AdminExhibitionCreateVO> create(@Validated @RequestBody AdminExhibitionSaveDTO dto) {
        log.info("后台新增展览，title：{}，galleryId：{}，展期：{} ~ {}，每日上限：{}",
                dto.getTitle(), dto.getGalleryId(), dto.getStartDate(), dto.getEndDate(), dto.getDailyStockLimit());
        return Result.success("新增成功", adminExhibitionService.create(dto));
    }

    /**
     * 编辑展览
     * <p>
     * 展期缩短时，超出新闭展日的日历库存会自动下线（已售出的只停售不删）
     */
    @PutMapping("/{id}")
    @ApiOperation("编辑展览")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody AdminExhibitionSaveDTO dto) {
        log.info("后台编辑展览，id：{}，title：{}", id, dto.getTitle());
        adminExhibitionService.update(id, dto);
        return Result.successMsg("修改成功");
    }

    /**
     * 删除展览
     * <p>
     * ⚠️ 有未完结订单的展览会被拒绝，提示改为下架
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除展览")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("后台删除展览，id：{}", id);
        adminExhibitionService.delete(id);
        return Result.successMsg("删除成功");
    }

    /**
     * 上下架 / 热门开关 / 排序（三个字段都可选，只更新传了的）
     */
    @PutMapping("/{id}/status")
    @ApiOperation("展览上下架/热门开关")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Validated @RequestBody AdminExhibitionStatusDTO dto) {
        log.info("后台修改展览状态，id：{}，status：{}，isHot：{}", id, dto.getStatus(), dto.getIsHot());
        adminExhibitionService.updateStatus(id, dto);
        return Result.successMsg("修改成功");
    }

    /**
     * 更新库存
     * <p>
     * 两种用法：只传 dailyStockLimit（改今天及以后的每日上限），
     * 或传 calendarAdjust（精确调某几天，新库存不得小于已售数量）
     */
    @PutMapping("/{id}/stock")
    @ApiOperation("更新展览库存")
    public Result<Void> updateStock(@PathVariable Long id,
                                    @Validated @RequestBody AdminExhibitionStockDTO dto) {
        log.info("后台更新展览库存，id：{}，dailyStockLimit：{}", id, dto.getDailyStockLimit());
        adminExhibitionService.updateStock(id, dto);
        return Result.successMsg("库存已更新");
    }
}
