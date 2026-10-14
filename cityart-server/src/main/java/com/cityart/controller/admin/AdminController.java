package com.cityart.controller.admin;

import com.cityart.context.AdminContext;
import com.cityart.dto.AdminLoginDTO;
import com.cityart.dto.AdminProfileDTO;
import com.cityart.result.Result;
import com.cityart.service.AdminService;
import com.cityart.vo.AdminProfileVO;
import com.cityart.vo.AdminVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端管理员接口（后台管理系统）
 * <pre>
 * POST /api/admin/login      管理员登录（免登录，已在拦截器放行）
 * GET  /api/admin/profile    当前登录管理员信息（页面刷新时用）
 * PUT  /api/admin/profile    编辑个人资料
 * </pre>
 * 除登录外都要求 token；adminId 一律从 {@link AdminContext} 取，**不由前端传**。
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "B端管理员接口")
@Slf4j
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 管理员登录
     * <p>
     * 账号密码错误 / 账号被禁用都返回 {"code":0,"msg":"..."}
     */
    @PostMapping("/login")
    @ApiOperation("管理员登录")
    public Result<AdminVO> login(@Validated @RequestBody AdminLoginDTO dto) {
        log.info("管理员登录，手机号：{}", dto.getPhone());
        return Result.success("登录成功", adminService.login(dto));
    }

    /**
     * 获取当前登录管理员信息
     * <p>
     * 前端页面刷新时只有 token，靠这个接口拉最新信息渲染（不要用本地缓存的用户信息）
     */
    @GetMapping("/profile")
    @ApiOperation("获取管理员信息")
    public Result<AdminProfileVO> profile() {
        Long adminId = AdminContext.getCurrentId();
        log.info("查询管理员信息，adminId：{}", adminId);
        return Result.success(adminService.getProfile(adminId));
    }

    /**
     * 编辑个人资料（姓名 / 头像 / 所在城市）
     * <p>
     * 手机号不支持修改，前端不传，后端也不处理
     */
    @PutMapping("/profile")
    @ApiOperation("编辑个人资料")
    public Result<Void> updateProfile(@Validated @RequestBody AdminProfileDTO dto) {
        Long adminId = AdminContext.getCurrentId();
        log.info("编辑管理员资料，adminId：{}", adminId);
        adminService.updateProfile(adminId, dto);
        return Result.successMsg("修改成功");
    }
}
