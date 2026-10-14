package com.cityart.service;

import com.cityart.dto.ProfileUpdateDTO;
import com.cityart.vo.PersonalCenterVO;

/**
 * 个人中心业务
 */
public interface PersonalService {

    /** 个人中心聚合信息（个人信息 + 三个统计） */
    PersonalCenterVO center(Long userId);

    /** 编辑个人资料（昵称 / 性别 / 城市，手机号不可改） */
    void updateProfile(Long userId, ProfileUpdateDTO dto);
}
