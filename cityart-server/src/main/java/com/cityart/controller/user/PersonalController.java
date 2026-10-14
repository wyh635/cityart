package com.cityart.controller.user;

import com.cityart.context.BaseContext;
import com.cityart.dto.ProfileUpdateDTO;
import com.cityart.result.Result;
import com.cityart.service.PersonalService;
import com.cityart.vo.PersonalCenterVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端个人中心接口（登录后访问）
 * GET /api/app/personal/center  个人中心聚合
 * PUT /api/app/profile          编辑个人资料
 */
@RestController
@RequestMapping("/api/app")
@Api(tags = "C端个人中心接口")
@Slf4j
public class PersonalController {

    @Autowired
    private PersonalService personalService;

    /**
     * 个人中心聚合信息（个人信息 + 我的收藏 / 有效票夹 / 观展次数）
     */
    @GetMapping("/personal/center")
    @ApiOperation("个人中心聚合信息")
    public Result<PersonalCenterVO> center() {
        Long userId = BaseContext.getCurrentId();
        log.info("个人中心，userId：{}", userId);
        return Result.success(personalService.center(userId));
    }

    /**
     * 编辑个人资料：昵称 / 性别 / 城市
     * <p>
     * ⚠️ 手机号不允许修改，前端不传、后端也忽略（DTO 里根本没有 phone 字段）
     */
    @PutMapping("/profile")
    @ApiOperation("编辑个人资料")
    public Result<Void> updateProfile(@Validated @RequestBody ProfileUpdateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("编辑个人资料，userId：{}，nickname：{}", userId, dto.getNickname());
        personalService.updateProfile(userId, dto);
        return Result.successMsg("修改成功");
    }
}
