package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员实体，对应 admin 表（B 端后台登录）
 * <p>
 * 字段与数据库一一对应；数据库下划线列名由 MyBatis 的
 * map-underscore-to-camel-case 自动映射（admin_code → adminCode）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    private Long id;

    /** 手机号（登录账号），11 位数字 */
    private String phone;

    /** MD5 加密后的密码密文 */
    private String password;

    /** 管理员姓名 */
    private String name;

    /** 头像 URL */
    private String avatar;

    /** 所在城市，如：南京 */
    private String city;

    /** 管理员编号（后台展示用） */
    private String adminCode;

    /** 账号状态：0-启用 1-禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
