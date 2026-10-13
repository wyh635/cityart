package com.cityart.controller.user;

import com.cityart.constant.MessageConstant;
import com.cityart.result.Result;
import com.cityart.service.HomeService;
import com.cityart.vo.BannerVO;
import com.cityart.vo.ExhibitionVO;
import com.cityart.vo.GalleryVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.List;

/**
 * C 端首页接口（4 个接口全部免登录，前端进入首页后并行调用）
 * GET /api/app/banner
 * GET /api/app/exhibition
 * GET /api/app/recommend
 * GET /api/app/gallery
 */
@RestController
@RequestMapping("/api/app")
@Api(tags = "C端首页接口")
@Slf4j
@Validated // 开启方法参数（@RequestParam）上的校验注解
public class HomeController {

    @Autowired
    private HomeService homeService;

    /**
     * 一、获取轮播图列表
     */
    @GetMapping("/banner")
    @ApiOperation("获取首页轮播图列表")
    public Result<List<BannerVO>> banner() {
        log.info("查询首页轮播图");
        return Result.success(homeService.listBanners());
    }

    /**
     * 二、获取当前热门展览
     *
     * @param city  城市，可不传
     * @param limit 返回条数上限，默认 10，最大 20
     */
    @GetMapping("/exhibition")
    @ApiOperation("获取当前热门展览")
    public Result<List<ExhibitionVO>> exhibition(
            @RequestParam(required = false)
            @Size(max = 50, message = "城市名称最多 50 个字符") String city,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "limit 不能小于 1")
            @Max(value = 20, message = "limit 不能大于 20") Integer limit) {
        log.info("查询当前热门展览，city：{}，limit：{}", city, limit);
        return Result.success(homeService.listHotExhibitions(city, limit));
    }

    /**
     * 三、获取「艺览智荐」推荐展览
     * 算法服务不可用时返回空数组，前端隐藏该区块，不影响首页其他 3 个区块
     *
     * @param userId 当前用户 ID，未登录不传
     * @param limit  返回条数，默认 4
     */
    @GetMapping("/recommend")
    @ApiOperation("获取艺览智荐推荐展览")
    public Result<List<ExhibitionVO>> recommend(
            @RequestParam(required = false)
            @Positive(message = "userId 必须为正整数") Long userId,
            @RequestParam(defaultValue = "4")
            @Min(value = 1, message = "limit 不能小于 1")
            @Max(value = 20, message = "limit 不能大于 20") Integer limit) {
        log.info("查询艺览智荐，userId：{}，limit：{}", userId, limit);
        try {
            return Result.success(homeService.listRecommendExhibitions(userId, limit));
        } catch (Exception e) {
            log.error("艺览智荐查询失败", e);
            return Result.error(MessageConstant.RECOMMEND_SERVICE_ERROR, Collections.emptyList());
        }
    }

    /**
     * 四、获取热门美术馆列表
     *
     * @param city  城市，可不传
     * @param limit 返回条数上限，默认 4（首页 2×2 宫格）
     */
    @GetMapping("/gallery")
    @ApiOperation("获取热门美术馆列表")
    public Result<List<GalleryVO>> gallery(
            @RequestParam(required = false)
            @Size(max = 50, message = "城市名称最多 50 个字符") String city,
            @RequestParam(defaultValue = "4")
            @Min(value = 1, message = "limit 不能小于 1")
            @Max(value = 20, message = "limit 不能大于 20") Integer limit) {
        log.info("查询热门美术馆，city：{}，limit：{}", city, limit);
        return Result.success(homeService.listHotGalleries(city, limit));
    }
}
