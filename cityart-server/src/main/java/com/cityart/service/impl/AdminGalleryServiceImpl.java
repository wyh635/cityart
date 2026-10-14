package com.cityart.service.impl;

import com.cityart.constant.MessageConstant;
import com.cityart.dto.AdminGalleryPageQueryDTO;
import com.cityart.dto.AdminGallerySaveDTO;
import com.cityart.enumeration.GalleryTypeEnum;
import com.cityart.exception.BaseException;
import com.cityart.mapper.GalleryMapper;
import com.cityart.result.PageResult;
import com.cityart.service.AdminGalleryService;
import com.cityart.vo.AdminGalleryDetailVO;
import com.cityart.vo.AdminGalleryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台「展馆管理」实现
 */
@Service
@Slf4j
public class AdminGalleryServiceImpl implements AdminGalleryService {

    /** 城市 → 展馆编号前缀 */
    private static final Map<String, String> CITY_CODE_PREFIX = new HashMap<>();

    static {
        CITY_CODE_PREFIX.put("南京", "NJ");
        CITY_CODE_PREFIX.put("苏州", "SZ");
        CITY_CODE_PREFIX.put("无锡", "WX");
    }

    private static final String DEFAULT_CITY_PREFIX = "EX";

    /** 状态文案 */
    private static final String STATUS_OPEN = "营业中";
    private static final String STATUS_CLOSED = "闭馆维护";

    @Autowired
    private GalleryMapper galleryMapper;

    // ==================================================================
    // 列表
    // ==================================================================

    @Override
    public PageResult<AdminGalleryVO> pageQuery(AdminGalleryPageQueryDTO dto) {
        // 空串当作「不筛选」，避免 city='' 查出 0 条
        dto.setCity(trimToNull(dto.getCity()));
        dto.setKeyword(trimToNull(dto.getKeyword()));

        long total = galleryMapper.countAdminGalleryPage(dto);

        List<AdminGalleryVO> list;
        if (total == 0) {
            list = new ArrayList<>();
        } else {
            list = galleryMapper.listAdminGalleryPage(dto);
            // 中文名与状态文案由后端给，前端不硬编码
            list.forEach(vo -> {
                vo.setTypeName(GalleryTypeEnum.labelOf(vo.getType()));
                vo.setStatusText(Integer.valueOf(1).equals(vo.getStatus()) ? STATUS_CLOSED : STATUS_OPEN);
            });
        }
        return PageResult.of(total, dto.getPage(), dto.getLimit(), list);
    }

    // ==================================================================
    // 详情
    // ==================================================================

    @Override
    public AdminGalleryDetailVO getDetailById(Long id) {
        AdminGalleryDetailVO vo = galleryMapper.getAdminGalleryById(id);
        if (vo == null) {
            throw new BaseException(MessageConstant.GALLERY_NOT_FOUND);
        }
        vo.setTypeName(GalleryTypeEnum.labelOf(vo.getType()));
        return vo;
    }

    // ==================================================================
    // 新增
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminGallerySaveDTO dto) {
        // 自动生成展馆编号：城市前缀 + G + 3 位序号（NJ-G007）
        String galleryCode = generateGalleryCode(dto.getCity());

        AdminGalleryDetailVO toInsert = AdminGalleryDetailVO.builder()
                .galleryCode(galleryCode)
                .name(dto.getName().trim())
                .city(dto.getCity().trim())
                .address(dto.getAddress().trim())
                .openHour(trimToNull(dto.getOpenHour()))
                .closeHour(trimToNull(dto.getCloseHour()))
                .closedDay(trimToNull(dto.getClosedDay()))
                // C 端展示用的 open_time 由后台三个结构化字段拼出来，运营不用手写
                .openTime(buildOpenTime(dto.getClosedDay(), dto.getOpenHour(), dto.getCloseHour()))
                .coverImage(dto.getCoverImage())
                .intro(dto.getIntro())
                .type(dto.getType() == null ? 1 : dto.getType())
                .isHot(dto.getIsHot() == null ? 0 : dto.getIsHot())
                .status(dto.getStatus() == null ? 0 : dto.getStatus())
                .build();

        galleryMapper.insertGallery(toInsert);

        log.info("新增展馆成功，id：{}，编号：{}，名称：{}，openTime：{}",
                toInsert.getId(), galleryCode, toInsert.getName(), toInsert.getOpenTime());

        return toInsert.getId();
    }

    // ==================================================================
    // 编辑
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminGallerySaveDTO dto) {
        AdminGalleryDetailVO exist = galleryMapper.getAdminGalleryById(id);
        if (exist == null) {
            throw new BaseException(MessageConstant.GALLERY_NOT_FOUND);
        }

        AdminGalleryDetailVO toUpdate = AdminGalleryDetailVO.builder()
                .id(id)
                .name(dto.getName().trim())
                .city(dto.getCity().trim())
                .address(dto.getAddress().trim())
                .openHour(trimToNull(dto.getOpenHour()))
                .closeHour(trimToNull(dto.getCloseHour()))
                .closedDay(trimToNull(dto.getClosedDay()))
                .openTime(buildOpenTime(dto.getClosedDay(), dto.getOpenHour(), dto.getCloseHour()))
                .coverImage(dto.getCoverImage())
                .intro(dto.getIntro())
                .type(dto.getType() == null ? exist.getType() : dto.getType())
                .isHot(dto.getIsHot() == null ? exist.getIsHot() : dto.getIsHot())
                .status(dto.getStatus() == null ? exist.getStatus() : dto.getStatus())
                .build();

        galleryMapper.updateGallery(toUpdate);
        log.info("编辑展馆成功，id：{}，名称：{}", id, toUpdate.getName());
    }

    // ==================================================================
    // 删除
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AdminGalleryDetailVO exist = galleryMapper.getAdminGalleryById(id);
        if (exist == null) {
            throw new BaseException(MessageConstant.GALLERY_NOT_FOUND);
        }

        // 🔑 校验一：名下还有展览 → 拒绝
        // 否则展览的 gallery_id 会指向一个不存在的馆，C 端详情页 JOIN 直接查不到
        int exhibitions = galleryMapper.countExhibitionsByGalleryId(id);
        if (exhibitions > 0) {
            throw new BaseException("该展馆下已有 " + exhibitions + " 个展览，不能删除");
        }

        // 🔑 校验二：名下有过订单 → 拒绝
        // 订单是财务数据，必须长期保留；场馆被删会让历史订单的场馆信息悬空
        int orders = galleryMapper.countOrdersByGalleryId(id);
        if (orders > 0) {
            throw new BaseException("该展馆已有 " + orders + " 笔订单记录，不能删除，请改为闭馆维护");
        }

        galleryMapper.deleteGallery(id);
        log.info("删除展馆成功，id：{}，名称：{}", id, exist.getName());
    }

    // ==================================================================
    // 私有工具方法
    // ==================================================================

    /**
     * 拼 C 端展示的开放时间
     * <p>
     * 规则（与 C 端已有的 openTime 数据格式保持一致）：
     * <ul>
     *     <li>有闭馆日：{@code 周二到周日 8:30-18:30}</li>
     *     <li>无闭馆日：{@code 8:30-18:30}</li>
     *     <li>时间缺失：返回 null（不拼出 "null-null" 这种脏数据）</li>
     * </ul>
     * 另外把 {@code 08:30} 这种带前导零的写法去掉零，与存量数据 {@code 8:30} 对齐。
     */
    private String buildOpenTime(String closedDay, String openHour, String closeHour) {
        String open = normalizeHour(openHour);
        String close = normalizeHour(closeHour);
        if (open == null || close == null) {
            return null;
        }
        String range = open + "-" + close;
        String day = trimToNull(closedDay);
        return day == null ? range : day + " " + range;
    }

    /** 08:30 → 8:30（去掉小时的前导零，与 C 端存量数据格式一致） */
    private String normalizeHour(String hour) {
        String value = trimToNull(hour);
        if (value == null) {
            return null;
        }
        // 匹配 H:mm 或 HH:mm，把小时的前导零去掉
        if (value.length() >= 4 && value.charAt(0) == '0' && Character.isDigit(value.charAt(1))) {
            return value.substring(1);
        }
        return value;
    }

    /**
     * 生成展馆编号：城市前缀 + "-G" + 3 位序号，如 NJ-G001
     * <p>
     * 取该城市已有编号的最大序号 +1；没有编号时从 1 开始。
     * ⚠️ 存量的 6 个馆 gallery_code 都是 NULL，所以第一次新增会得到 NJ-G001。
     */
    private String generateGalleryCode(String city) {
        String prefix = CITY_CODE_PREFIX.getOrDefault(city, DEFAULT_CITY_PREFIX);
        Integer maxSeq = galleryMapper.getMaxGalleryCodeSeqOfCity(city);
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + "-G" + String.format("%03d", next);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
