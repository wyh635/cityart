package com.cityart.controller.user;

import com.cityart.result.Result;
import com.cityart.service.DictService;
import com.cityart.vo.DictItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端字典接口（免登录）
 * GET /api/app/dict/exhibition-types
 * GET /api/app/dict/gallery-types
 * GET /api/app/dict/cities
 * <p>
 * 中文名统一由后端维护，前端不要硬编码；「全部类型」不占值，不传 type 参数即可。
 */
@RestController
@RequestMapping("/api/app/dict")
@Api(tags = "C端字典接口")
@Slf4j
public class DictController {

    @Autowired
    private DictService dictService;

    /**
     * 展览类型字典（「全部展览」页下拉）
     */
    @GetMapping("/exhibition-types")
    @ApiOperation("展览类型字典")
    public Result<List<DictItemVO>> exhibitionTypes() {
        log.info("查询展览类型字典");
        return Result.success(dictService.listExhibitionTypes());
    }

    /**
     * 美术馆类型字典（「美术馆」页下拉）
     */
    @GetMapping("/gallery-types")
    @ApiOperation("美术馆类型字典")
    public Result<List<DictItemVO>> galleryTypes() {
        log.info("查询美术馆类型字典");
        return Result.success(dictService.listGalleryTypes());
    }

    /**
     * 城市字典：只返回有正常营业美术馆的城市
     */
    @GetMapping("/cities")
    @ApiOperation("城市字典")
    public Result<List<String>> cities() {
        log.info("查询城市字典");
        return Result.success(dictService.listCities());
    }
}
