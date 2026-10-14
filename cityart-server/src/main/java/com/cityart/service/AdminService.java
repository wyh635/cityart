package com.cityart.service;

import com.cityart.dto.AdminLoginDTO;
import com.cityart.dto.AdminProfileDTO;
import com.cityart.vo.AdminProfileVO;
import com.cityart.vo.AdminVO;

/**
 * B 端管理员业务
 */
public interface AdminService {

    /**
     * 管理员登录
     *
     * @return 含 B 端 token 的信息；账号不存在 / 密码错 / 被禁用都抛业务异常
     */
    AdminVO login(AdminLoginDTO dto);

    /**
     * 获取当前登录管理员的个人信息（页面刷新时用）
     *
     * @param adminId 从 AdminContext 取，不由前端传
     */
    AdminProfileVO getProfile(Long adminId);

    /**
     * 修改当前登录管理员的个人资料（姓名 / 头像 / 城市）
     * 手机号不允许修改
     */
    void updateProfile(Long adminId, AdminProfileDTO dto);
}
