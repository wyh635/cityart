package com.cityart.controller.user;

import com.cityart.context.BaseContext;
import com.cityart.dto.FavoriteDTO;
import com.cityart.dto.FavoritePageQueryDTO;
import com.cityart.result.Result;
import com.cityart.service.FavoriteService;
import com.cityart.vo.FavoriteCheckVO;
import com.cityart.vo.FavoritePageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * C 端收藏接口（登录后访问）
 * GET    /api/app/favorites/page   收藏列表
 * POST   /api/app/favorites        添加收藏
 * DELETE /api/app/favorites        取消收藏
 * GET    /api/app/favorites/check  收藏状态
 */
@RestController
@RequestMapping("/api/app/favorites")
@Api(tags = "C端收藏接口")
@Slf4j
@Validated
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 收藏列表分页（顶部「展览 / 美术馆」双 tab，默认 4 条铺满 2×2）
     */
    @GetMapping("/page")
    @ApiOperation("收藏列表分页")
    public Result<FavoritePageVO<Object>> page(@Validated FavoritePageQueryDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("收藏列表，userId：{}，targetType：{}，page：{}/{}", userId, dto.getTargetType(), dto.getPage(), dto.getPageSize());
        return Result.success(favoriteService.pageFavorites(userId, dto));
    }

    /**
     * 添加收藏（幂等：重复收藏直接返回成功）
     */
    @PostMapping
    @ApiOperation("添加收藏")
    public Result<Void> add(@Validated @RequestBody FavoriteDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("添加收藏，userId：{}，targetType：{}，targetId：{}", userId, dto.getTargetType(), dto.getTargetId());
        favoriteService.add(userId, dto);
        return Result.successMsg("收藏成功");
    }

    /**
     * 取消收藏
     */
    @DeleteMapping
    @ApiOperation("取消收藏")
    public Result<Void> remove(@Validated @RequestBody FavoriteDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("取消收藏，userId：{}，targetType：{}，targetId：{}", userId, dto.getTargetType(), dto.getTargetId());
        favoriteService.remove(userId, dto);
        return Result.successMsg("已取消收藏");
    }

    /**
     * 查询收藏状态（详情页判断按钮显示「收藏」还是「已收藏」）
     */
    @GetMapping("/check")
    @ApiOperation("查询收藏状态")
    public Result<FavoriteCheckVO> check(
            @RequestParam @NotNull(message = "targetType 不能为空")
            @Min(value = 1, message = "targetType 只能是 1-展览 2-美术馆")
            @Max(value = 2, message = "targetType 只能是 1-展览 2-美术馆") Integer targetType,
            @RequestParam @NotNull(message = "targetId 不能为空")
            @Min(value = 1, message = "targetId 必须为正整数") Long targetId) {
        Long userId = BaseContext.getCurrentId();
        boolean favorited = favoriteService.check(userId, targetType, targetId);
        return Result.success(new FavoriteCheckVO(favorited));
    }
}
