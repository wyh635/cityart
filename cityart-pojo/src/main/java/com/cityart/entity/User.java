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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status;

}
