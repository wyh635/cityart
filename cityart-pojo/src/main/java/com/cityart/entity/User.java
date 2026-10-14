package com.cityart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String phone;
    private String password; //数据库存加密后的密码，不要明文
    private String nickname;
    private String avatar;        //头像地址
    private String city;          //所在城市（个人中心昵称下方显示）
    private Integer gender;       //0-未知 1-男 2-女
    private Integer visitCount;   //观展次数（冗余字段，核销时 +1）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status;

}
