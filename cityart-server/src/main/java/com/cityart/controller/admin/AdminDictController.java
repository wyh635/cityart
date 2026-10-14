package com.cityart.controller.admin;

import com.cityart.result.Result;
import com.cityart.service.DictService;
import com.cityart.vo.DictItemVO;
import com.cityart.vo.GalleryOptionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * B 端通用字典接口
 * <pre>
 * GET /api/admin/dict/cities             城市下拉（读 city 表）
 * GET /api/admin/dict/exhibition-types   展览类型下拉（1→5 正序）
 * GET /api/admin/dict/gallery-types      展馆类型下拉（1→5 正序）
 * GET /api/admin/dict/galleries          「所属展馆」下拉（可按 city 过滤）
 * </pre>
 * 中文名统一由后端维护，前端不要硬编码；
 * 「全部类型 / 全部城市」不占 value，由前端加为第一项，选中时不传对应参数。
 */
@RestController
@RequestMapping("/api/admin/dict")
@Api(tags = "B端字典接口")
@Slf4j
public class AdminDictController {

    @Autowired
    private DictService dictService;

    /**
     * 城市下拉
     * <p>
     * ⚠️ 与 C 端 `/api/app/dict/cities` 的数据来源不同：
     * 这里查 {@code city} 表，运营在一个展馆都还没建时也能选到城市；
     * C 端查 {@code DISTINCT gallery.city}，只会返回有营业展馆的城市。
     */
    @GetMapping("/cities")
    @ApiOperation("城市下拉（B端）")
    public Result<List<String>> cities() {
        log.info("查询B端城市字典");
        return Result.success(dictService.listAdminCities());
    }

    /**
     * 展览类型下拉（1→5 正序，与 C 端的倒序不同）
     */
    @GetMapping("/exhibition-types")
    @ApiOperation("展览类型下拉（B端）")
    public Result<List<DictItemVO>> exhibitionTypes() {
        log.info("查询B端展览类型字典");
        return Result.success(dictService.listExhibitionTypesAsc());
    }

    /**
     * 展馆类型下拉（1→5 正序）
     */
    @GetMapping("/gallery-types")
    @ApiOperation("展馆类型下拉（B端）")
    public Result<List<DictItemVO>> galleryTypes() {
        log.info("查询B端展馆类型字典");
        return Result.success(dictService.listGalleryTypesAsc());
    }

    /**
     * 「所属展馆」下拉（新增展览页）
     *
     * @param city 可选。选了城市后只列该城市的馆，避免运营选到外地的馆
     */
    @GetMapping("/galleries")
    @ApiOperation("所属展馆下拉")
    public Result<List<GalleryOptionVO>> galleries(@RequestParam(required = false) String city) {
        log.info("查询所属展馆下拉，city：{}", city);
        return Result.success(dictService.listGalleryOptions(city));
    }
}
