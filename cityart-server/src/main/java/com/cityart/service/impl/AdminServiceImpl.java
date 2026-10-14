package com.cityart.service.impl;

import com.cityart.constant.JwtClaimsConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.constant.StatusConstant;
import com.cityart.dto.AdminLoginDTO;
import com.cityart.dto.AdminProfileDTO;
import com.cityart.entity.Admin;
import com.cityart.exception.AccountLockedException;
import com.cityart.exception.BaseException;
import com.cityart.exception.LoginFailedException;
import com.cityart.exception.PasswordErrorException;
import com.cityart.mapper.AdminMapper;
import com.cityart.properties.JwtProperties;
import com.cityart.service.AdminService;
import com.cityart.utils.JwtUtil;
import com.cityart.vo.AdminProfileVO;
import com.cityart.vo.AdminVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * B 端管理员业务实现
 * <p>
 * ⚠️ 与 C 端 {@code UserServiceImpl} 的三处关键差异：
 * <ol>
 *     <li>查的是 <b>admin 表</b>（不是 user 表）</li>
 *     <li>签发用 <b>adminSecretKey / adminTtl</b>（不是 user 的）</li>
 *     <li>claims 键用 <b>ADMIN_ID</b>（不是 USER_ID）—— 键名错了 B 端拦截器取不到 id</li>
 * </ol>
 */
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public AdminVO login(AdminLoginDTO dto) {
        // 1. 按手机号查管理员
        Admin admin = adminMapper.getByPhone(dto.getPhone());
        if (admin == null) {
            // 统一模糊提示，不暴露「账号不存在」还是「密码错误」
            throw new LoginFailedException("账号或密码错误");
        }

        // 2. 比对密码（与 C 端一致：MD5，学习项目够用；生产建议 BCrypt 加盐）
        String encryptPwd = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes());
        if (!encryptPwd.equals(admin.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        // 3. 账号是否被禁用
        if (StatusConstant.DISABLE.equals(admin.getStatus())) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        // 4. 签发 B 端 token
        String token = createToken(admin.getId());

        log.info("管理员登录成功，adminId：{}，name：{}", admin.getId(), admin.getName());

        // 5. 组装返回（不含 password）
        return AdminVO.builder()
                .id(admin.getId())
                .phone(admin.getPhone())
                .name(admin.getName())
                .avatar(admin.getAvatar())
                .city(admin.getCity())
                .token(token)
                .build();
    }

    @Override
    public AdminProfileVO getProfile(Long adminId) {
        if (adminId == null) {
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }
        Admin admin = adminMapper.getById(adminId);
        if (admin == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        return AdminProfileVO.builder()
                .id(admin.getId())
                .name(admin.getName())
                .phone(maskPhone(admin.getPhone()))   // 脱敏后返回，不下发完整手机号
                .avatar(admin.getAvatar())
                .city(admin.getCity())
                .adminCode(admin.getAdminCode())
                .status(admin.getStatus())
                .createTime(admin.getCreateTime())
                .build();
    }

    @Override
    public void updateProfile(Long adminId, AdminProfileDTO dto) {
        if (adminId == null) {
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }
        // 三个字段全空时不更新：Mapper 用的是动态 <set>，
        // 全空会生成 "update admin where id = ?" 这种非法 SQL
        boolean nameBlank = isBlank(dto.getName());
        boolean avatarBlank = isBlank(dto.getAvatar());
        boolean cityBlank = isBlank(dto.getCity());
        if (nameBlank && avatarBlank && cityBlank) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        // 只更新传了的字段；手机号与密码不在本接口范围内
        Admin update = Admin.builder()
                .id(adminId)
                .name(nameBlank ? null : dto.getName().trim())
                .avatar(avatarBlank ? null : dto.getAvatar().trim())
                .city(cityBlank ? null : dto.getCity().trim())
                .build();
        adminMapper.updateProfile(update);
    }

    /** null / 空串 / 全空格 都算空 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 签发 B 端 token
     * <p>
     * 🔑 三个「必须是 admin 的」：密钥、过期时间、claims 键。
     * 用成 user 的任意一个，B 端拦截器都会验不过或取不到 adminId。
     */
    private String createToken(Long adminId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.ADMIN_ID, adminId);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);
    }

    /** 手机号脱敏：13800138000 → 138****8000 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
