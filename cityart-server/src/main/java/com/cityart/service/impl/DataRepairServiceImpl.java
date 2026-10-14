package com.cityart.service.impl;

import com.cityart.mapper.ExhibitionCalendarMapper;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.mapper.UserMapper;
import com.cityart.service.DataRepairService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 冗余字段校准实现
 * <p>
 * 每个 mapper 方法内部都带 `where 现值 <> 应为值` 条件，
 * 所以「影响行数」就是真实漂移的行数，可以直接作为校准结果返回。
 */
@Service
@Slf4j
public class DataRepairServiceImpl implements DataRepairService {

    @Autowired
    private ExhibitionMapper exhibitionMapper;

    @Autowired
    private ExhibitionCalendarMapper calendarMapper;

    @Autowired
    private GalleryMapper galleryMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 全部放在一个事务里：这几个字段互相独立，但一起成功更像「一次校准快照」，
     * 避免校准到一半时数据处于半新半旧的状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> repairAll() {
        Map<String, Integer> result = new LinkedHashMap<>();

        // 顺序有讲究：先修票种（exhibition.total_stock / price 依赖它）
        result.put("ticketTypeSoldCount", exhibitionMapper.repairTicketTypeSoldCount());
        result.put("calendarSoldCount", calendarMapper.repairAllSoldCount());
        result.put("exhibitionDenormalized", exhibitionMapper.repairExhibitionDenormalized());
        result.put("galleryExhibitionCount", galleryMapper.repairAllExhibitionCount());
        result.put("userVisitCount", userMapper.repairAllVisitCount());

        int total = result.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            log.info("【数据校准】所有冗余字段一致，无需修正");
        } else {
            log.warn("【数据校准】发现并修正了 {} 行漂移数据：{}", total, result);
        }
        return result;
    }
}
