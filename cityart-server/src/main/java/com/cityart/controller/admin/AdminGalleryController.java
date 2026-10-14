package com.cityart.controller.admin;

import com.cityart.dto.AdminGalleryPageQueryDTO;
import com.cityart.dto.AdminGallerySaveDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.AdminGalleryService;
import com.cityart.vo.AdminGalleryDetailVO;
import com.cityart.vo.AdminGalleryVO;
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

import java.util.Collections;
import java.util.Map;

/**
 * B 端「展馆管理」接口
 * <pre>
 * POST   /api/admin/galleries            新增展馆
 * GET    /api/admin/galleries/page       展馆列表
 * GET    /api/admin/galleries/{id}       展馆详情（编辑回填）
 * PUT    /api/admin/galleries/{id}       编辑展馆
 * DELETE /api/admin/galleries/{id}       删除展馆（名下有展览或订单则拒绝）
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/galleries")
@Api(tags = "B端展馆管理接口")
@Slf4j
public class AdminGalleryController {

    @Autowired
    private AdminGalleryService adminGalleryService;

    /**
     * 新增展馆
     * <p>
     * 后端自动做两件事：生成展馆编号（如 NJ-G007）、
     * 由「闭馆日 + 开馆时间 + 闭馆时间」拼出 C 端展示的 openTime
     */
    @PostMapping
    @ApiOperation("新增展馆")
    public Result<Map<String, Long>> create(@Validated @RequestBody AdminGallerySaveDTO dto) {
        log.info("后台新增展馆，name：{}，city：{}", dto.getName(), dto.getCity());
        Long id = adminGalleryService.create(dto);
        return Result.success("新增成功", Collections.singletonMap("id", id));
    }

    /**
     * 展馆列表分页
     * <p>
     * 筛选：城市 / 类型 / 状态 / 搜索（名称或编号）
     */
    @GetMapping("/page")
    @ApiOperation("展馆列表分页")
    public Result<PageResult<AdminGalleryVO>> page(@Validated AdminGalleryPageQueryDTO dto) {
        log.info("后台查询展馆列表，city：{}，type：{}，status：{}，keyword：{}，page：{}/{}",
                dto.getCity(), dto.getType(), dto.getStatus(), dto.getKeyword(),
                dto.getPage(), dto.getPageSize());
        return Result.success(adminGalleryService.pageQuery(dto));
    }

    /**
     * 展馆详情（编辑回填）
     * <p>
     * 注意：与 C 端不同，这里**闭馆维护的馆也能查到**，否则后台没法把它改回营业
     */
    @GetMapping("/{id}")
    @ApiOperation("展馆详情")
    public Result<AdminGalleryDetailVO> detail(@PathVariable Long id) {
        log.info("后台查询展馆详情，id：{}", id);
        return Result.success(adminGalleryService.getDetailById(id));
    }

    /**
     * 编辑展馆
     */
    @PutMapping("/{id}")
    @ApiOperation("编辑展馆")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody AdminGallerySaveDTO dto) {
        log.info("后台编辑展馆，id：{}，name：{}", id, dto.getName());
        adminGalleryService.update(id, dto);
        return Result.successMsg("修改成功");
    }

    /**
     * 删除展馆
     * <p>
     * ⚠️ 名下有展览或有过订单会被拒绝，提示改为闭馆维护
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除展馆")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("后台删除展馆，id：{}", id);
        adminGalleryService.delete(id);
        return Result.successMsg("删除成功");
    }
}
