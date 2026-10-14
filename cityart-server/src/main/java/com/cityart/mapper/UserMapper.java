package com.cityart.mapper;

import com.cityart.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    /** 根据手机号查询用户 */
    @Select("select * from user where phone = #{phone}")
    User getByPhone(String phone);

    /** 根据ID查询用户（个人中心、编辑资料用） */
    @Select("select * from user where id = #{id}")
    User getById(@Param("id") Long id);

    /** 新增用户 */
    @Insert("insert into user(phone,password,nickname,create_time) " +
            "values(#{phone},#{password},#{nickname},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    /**
     * 修改个人资料：只允许改昵称 / 性别 / 城市
     * <p>
     * ⚠️ SQL 里**故意不包含 phone 与 password**：手机号不允许修改，
     * 就算前端多传了 phone 也不会被写库
     */
    @Update("update user set nickname = #{nickname}, gender = #{gender}, city = #{city} where id = #{id}")
    void updateProfile(User user);

    /**
     * 观展次数 +1（核销入场成功后调用）
     * <p>
     * user.visit_count 是**冗余字段**，个人中心「观展次数」直接读它，
     * 避免每次进个人中心都去 COUNT 订单表。
     */
    @Update("update user set visit_count = visit_count + 1 where id = #{id}")
    int increaseVisitCount(@Param("id") Long id);

    /**
     * 校准所有用户的观展次数（全表重算，只更新对不上的行）
     *
     * @return 实际被修正的行数
     */
    @Update("update user u set u.visit_count = (" +
            "  select count(*) from exhibition_order o " +
            "  where o.user_id = u.id and o.use_time is not null" +
            ") where u.visit_count <> (" +
            "  select count(*) from exhibition_order o " +
            "  where o.user_id = u.id and o.use_time is not null" +
            ")")
    int repairAllVisitCount();
}
