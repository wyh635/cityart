package com.cityart.controller.user;

import com.cityart.dto.GalleryExhibitionPageQueryDTO;
import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.GalleryService;
import com.cityart.vo.GalleryDetailVO;
import com.cityart.vo.GalleryExhibitionVO;
import com.cityart.vo.GalleryPageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端美术馆接口（全部免登录）
 * GET /api/app/galleries/page            美术馆分页列表
 * GET /api/app/galleries/{id}            美术馆详情
 * GET /api/app/galleries/{id}/exhibitions 馆内展览列表（正在展出 / 往期展出）
 */
@RestController
@RequestMapping("/api/app/galleries")
@Api(tags = "C端美术馆接口")
@Slf4j
public class GalleryController {

    @Autowired
    private GalleryService galleryService;

    /**
     * 美术馆分页列表（只返回正常营业的馆，默认每页 6 个）
     */
    @GetMapping("/page")
    @ApiOperation("美术馆分页列表")
    public Result<PageResult<GalleryPageVO>> page(@Validated GalleryPageQueryDTO dto) {
        log.info("查询美术馆列表，type：{}，city：{}，keyword：{}，page：{}/{}",
                dto.getType(), dto.getCity(), dto.getKeyword(), dto.getPage(), dto.getPageSize());
        return Result.success(galleryService.pageQuery(dto));
    }

    /**
     * 美术馆详情（顶部信息 + 展馆简介 + 两个分组的展览数量）
     * 查不到时返回 {"code":0,"msg":"美术馆不存在"}
     */
    @GetMapping("/{id}")
    @ApiOperation("美术馆详情")
    public Result<GalleryDetailVO> detail(@PathVariable Long id) {
        log.info("查询美术馆详情，id：{}", id);
        return Result.success(galleryService.getDetailById(id));
    }

    /**
     * 美术馆详情页的展览列表（「正在展出 CURRENT」和「往期展出 PAST」靠 status 区分）
     */
    @GetMapping("/{id}/exhibitions")
    @ApiOperation("馆内展览分页列表")
    public Result<PageResult<GalleryExhibitionVO>> exhibitions(@PathVariable Long id,
                                                              @Validated GalleryExhibitionPageQueryDTO dto) {
        log.info("查询馆内展览，galleryId：{}，status：{}，page：{}/{}",
                id, dto.getStatus(), dto.getPage(), dto.getPageSize());
        return Result.success(galleryService.pageExhibitions(id, dto));
    }
}
