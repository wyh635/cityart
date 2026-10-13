package com.cityart.controller.user;

import com.cityart.dto.UserLoginDTO;
import com.cityart.result.Result;
import com.cityart.service.UserService;
import com.cityart.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@Api(tags = "C端手机号注册登录接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 注册接口 POST /api/app/register
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result<UserVO> register (@Validated @RequestBody UserLoginDTO dto){
        log.info("用户注册，手机号：{}", dto.getPhone());
        UserVO vo = userService.register(dto);
        return Result.success(vo);
    }

    /**
     * 登录接口 POST /api/app/login
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<UserVO> login (@Validated @RequestBody UserLoginDTO dto){
        log.info("用户登录，手机号：{}", dto.getPhone());
        UserVO vo = userService.login(dto);
        return Result.success(vo);
    }
}
