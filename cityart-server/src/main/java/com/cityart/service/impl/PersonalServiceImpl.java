package com.cityart.service.impl;

import com.cityart.constant.FavoriteTargetTypeConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.dto.ProfileUpdateDTO;
import com.cityart.entity.User;
import com.cityart.exception.BaseException;
import com.cityart.mapper.ExhibitionOrderMapper;
import com.cityart.mapper.FavoriteMapper;
import com.cityart.mapper.UserMapper;
import com.cityart.service.PersonalService;
import com.cityart.utils.PhoneUtil;
import com.cityart.vo.PersonalCenterVO;
import com.cityart.vo.PersonalUserVO;
import com.cityart.vo.UserStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PersonalServiceImpl implements PersonalService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ExhibitionOrderMapper orderMapper;

    @Override
    public PersonalCenterVO center(Long userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 手机号一律脱敏后再下发
        PersonalUserVO userVO = PersonalUserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(PhoneUtil.mask(user.getPhone()))
                .city(user.getCity())
                .gender(user.getGender())
                .build();

        // 三个统计：收藏（展览 + 美术馆）、有效票夹（已支付订单）、观展次数（冗余字段）
        int favoriteCount = favoriteMapper.countByUserAndType(userId, FavoriteTargetTypeConstant.EXHIBITION)
                + favoriteMapper.countByUserAndType(userId, FavoriteTargetTypeConstant.GALLERY);
        UserStatisticsVO statisticsVO = UserStatisticsVO.builder()
                .favoriteCount(favoriteCount)
                .validTicketCount(orderMapper.countValidTickets(userId))
                .visitCount(user.getVisitCount() == null ? 0 : user.getVisitCount())
                .build();

        return PersonalCenterVO.builder().user(userVO).statistics(statisticsVO).build();
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateDTO dto) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 只覆盖前端传了的字段，没传的保持原值（手机号、密码根本不参与）
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getCity() != null) {
            user.setCity(dto.getCity());
        }
        userMapper.updateProfile(user);
        log.info("修改个人资料，userId：{}", userId);
    }
}
