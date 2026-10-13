package com.cityart.service;

import com.cityart.dto.UserLoginDTO;
import com.cityart.vo.UserVO;

public interface UserService {
    /**
     * 用户注册
     */
    UserVO register(UserLoginDTO dto);
    /**
     * 用户登录
     *
     * @param userLoginDTO
     * @return
     */
    UserVO login(UserLoginDTO userLoginDTO);
}
