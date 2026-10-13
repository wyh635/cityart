package com.cityart.mapper;

import com.cityart.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /** 根据手机号查询用户 */
    @Select("select * from user where phone = #{phone}")
    User getByPhone(String phone);

    /** 新增用户 */
    @Insert("insert into user(phone,password,nickname,create_time) " +
            "values(#{phone},#{password},#{nickname},#{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
}
