package com.cityart.service.impl;

import com.cityart.constant.JwtClaimsConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.constant.StatusConstant;
import com.cityart.dto.UserLoginDTO;
import com.cityart.entity.User;
import com.cityart.exception.AccountLockedException;
import com.cityart.exception.LoginFailedException;
import com.cityart.exception.PasswordErrorException;
import com.cityart.mapper.UserMapper;
import com.cityart.properties.JwtProperties;
import com.cityart.service.UserService;
import com.cityart.utils.JwtUtil;
import com.cityart.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public UserVO register(UserLoginDTO dto) {
        //1. 判断手机号是否已经注册
        User existUser = userMapper.getByPhone(dto.getPhone());
        if(existUser != null){
            throw new LoginFailedException("该手机号已注册");
        }

        //2.密码加密
        String encryptPwd = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes());

        //3.构建用户对象存入数据库，昵称示例：138****8000
        String phone = dto.getPhone();
        String nickName = phone.substring(0,3)+"****"+phone.substring(7);

        User user = User.builder()
                .phone(phone)
                .password(encryptPwd)
                .nickname(nickName)
                .createTime(LocalDateTime.now())
                .build();
        userMapper.insert(user);

        //4.注册成功生成token
        String token = createToken(user.getId());

        //5.组装VO返回
        return UserVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .token(token)
                .build();
    }


    public UserVO login(UserLoginDTO dto) {
        //1.根据手机号查询
        User user = userMapper.getByPhone(dto.getPhone());
        if(user == null){
            throw new LoginFailedException("账号或密码错误");
        }
        //2.比对密码
        String password = dto.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(user.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (user.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3.生成token
        String token = createToken(user.getId());

        return UserVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .token(token)
                .build();
    }

    /** 抽取公共生成token方法 */
    private String createToken(Long userId){
        Map<String,Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, userId);
        return JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
    }
}




