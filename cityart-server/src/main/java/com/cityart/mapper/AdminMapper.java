package com.cityart.mapper;

import com.cityart.entity.Admin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 管理员 Mapper（B 端）
 * <p>
 * ⚠️ 前置：数据库里要有 admin 表的 avatar / city / admin_code 三列，
 * 即先执行 db/07_admin_tables.sql，否则查询会报 Unknown column。
 */
@Mapper
public interface AdminMapper {

    /**
     * 根据手机号查询管理员（登录用）
     *
     * @return 查不到返回 null，由 Service 抛「账号或密码错误」
     */
    @Select("select id, phone, password, name, avatar, city, admin_code, status, create_time, update_time " +
            "from admin where phone = #{phone}")
    Admin getByPhone(@Param("phone") String phone);

    /**
     * 根据 id 查询管理员（个人中心 / 刷新页面时拉最新信息）
     */
    @Select("select id, phone, password, name, avatar, city, admin_code, status, create_time, update_time " +
            "from admin where id = #{id}")
    Admin getById(@Param("id") Long id);

    /**
     * 修改个人资料（姓名 / 头像 / 城市）
     * <p>
     * ⚠️ 必须用动态 SQL：前端可能只改其中一项。
     * 若写成无条件 `set name=?, avatar=?, city=?`，只改昵称时会把 avatar 更新成 NULL，
     * 而 avatar 是 NOT NULL 列 → 报 Column 'avatar' cannot be null。
     * <p>
     * 三项全为空时 <set> 标签会生成非法 SQL，所以在 Service 层已做兜底校验。
     */
    @Update("<script>" +
            "update admin " +
            "<set>" +
            "  <if test='name != null and name != \"\"'> name = #{name}, </if>" +
            "  <if test='avatar != null'> avatar = #{avatar}, </if>" +
            "  <if test='city != null and city != \"\"'> city = #{city}, </if>" +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    int updateProfile(Admin admin);

    /**
     * 修改密码（忘记密码 / 重置密码）
     */
    @Update("update admin set password = #{password} where phone = #{phone}")
    int updatePasswordByPhone(@Param("phone") String phone, @Param("password") String password);

    /**
     * 新增管理员（后台手动添加，无对外注册入口）
     */
    @Insert("insert into admin(phone, password, name, avatar, city, admin_code, status, create_time) " +
            "values(#{phone}, #{password}, #{name}, #{avatar}, #{city}, #{adminCode}, #{status}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Admin admin);
}
