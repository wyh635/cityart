package com.cityart.controller.user;

import com.cityart.dto.GalleryPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.GalleryService;
import com.cityart.vo.GalleryPageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端「美术馆」列表接口（免登录）
 * GET /api/app/galleries/page
 */
@RestController
@RequestMapping("/api/app/galleries")
@Api(tags = "C端美术馆列表接口")
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
}
