package com.cityart.service.impl;

import com.cityart.constant.MessageConstant;
import com.cityart.dto.AdminExhibitionPageQueryDTO;
import com.cityart.dto.AdminExhibitionSaveDTO;
import com.cityart.dto.AdminExhibitionStatusDTO;
import com.cityart.dto.AdminExhibitionStockDTO;
import com.cityart.entity.TicketType;
import com.cityart.enumeration.ExhibitionTypeEnum;
import com.cityart.exception.BaseException;
import com.cityart.mapper.ExhibitionCalendarMapper;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.mapper.TicketTypeMapper;
import com.cityart.result.PageResult;
import com.cityart.service.AdminExhibitionService;
import com.cityart.vo.AdminExhibitionCreateVO;
import com.cityart.vo.AdminExhibitionDetailVO;
import com.cityart.vo.AdminExhibitionVO;
import com.cityart.vo.GalleryDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台「展览管理」实现
 */
@Service
@Slf4j
public class AdminExhibitionServiceImpl implements AdminExhibitionService {

    /** 城市 → 展览编号前缀（原型上的 NJ001 就是南京） */
    private static final Map<String, String> CITY_CODE_PREFIX = new HashMap<>();

    static {
        CITY_CODE_PREFIX.put("南京", "NJ");
        CITY_CODE_PREFIX.put("苏州", "SZ");
        CITY_CODE_PREFIX.put("无锡", "WX");
    }

    private static final String DEFAULT_CITY_PREFIX = "EX";

    @Autowired
    private ExhibitionMapper exhibitionMapper;

    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Autowired
    private ExhibitionCalendarMapper exhibitionCalendarMapper;

    @Autowired
    private GalleryMapper galleryMapper;

    // ==================================================================
    // 列表
    // ==================================================================

    @Override
    public PageResult<AdminExhibitionVO> pageQuery(AdminExhibitionPageQueryDTO dto) {
        // 1. 空串当作「不筛选」，避免 city='' 查出 0 条
        dto.setCity(trimToNull(dto.getCity()));
        dto.setKeyword(trimToNull(dto.getKeyword()));
        String stockStatus = trimToNull(dto.getStockStatus());

        long total = exhibitionMapper.countAdminExhibitionPage(dto);

        List<AdminExhibitionVO> list = total == 0
                ? new ArrayList<>()
                : exhibitionMapper.listAdminExhibitionPage(dto);

        // 2. 批量回填「当日库存」——一次查完，不在循环里查（N+1）
        fillDailyStock(list);

        // 3. 库存状态筛选（available / soldOut）
        //    ⚠️ 这个条件依赖日历库存，不是数据库列，所以只能在 Service 层过滤。
        //       若在此处过滤，pageSize 会与实际返回条数不一致，需前端知悉；
        //       精确分页应在 SQL 里用子查询，这里按当前数据量做简化处理。
        if ("available".equals(stockStatus)) {
            list = list.stream()
                    .filter(vo -> !"soldOut".equals(vo.getStockStatus()))
                    .collect(Collectors.toList());
        } else if ("soldOut".equals(stockStatus)) {
            list = list.stream()
                    .filter(vo -> "soldOut".equals(vo.getStockStatus()))
                    .collect(Collectors.toList());
        }

        return PageResult.of(total, dto.getPage(), dto.getLimit(), list);
    }

    /**
     * 批量回填当日库存：取每个展览「最近放票日」各票种剩余之和
     */
    private void fillDailyStock(List<AdminExhibitionVO> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(AdminExhibitionVO::getId).collect(Collectors.toList());
        List<AdminExhibitionVO> stocks = exhibitionMapper.listNearestDailyStock(ids);

        Map<Long, Integer> stockMap = new HashMap<>();
        for (AdminExhibitionVO s : stocks) {
            stockMap.put(s.getId(), s.getDailyStock() == null ? 0 : s.getDailyStock());
        }
        for (AdminExhibitionVO vo : list) {
            int stock = stockMap.getOrDefault(vo.getId(), 0);
            vo.setDailyStock(stock);
            // 原型「已售完」= 剩余 0，否则显示「N张」
            vo.setStockStatus(stock <= 0 ? "soldOut" : "available");
            vo.setStockStatusText(stock <= 0 ? "已售完" : stock + "张");
        }
    }

    // ==================================================================
    // 详情
    // ==================================================================

    @Override
    public AdminExhibitionDetailVO getDetailById(Long id) {
        AdminExhibitionDetailVO vo = exhibitionMapper.getAdminExhibitionById(id);
        if (vo == null) {
            throw new BaseException(MessageConstant.EXHIBITION_NOT_FOUND);
        }
        vo.setTypeName(ExhibitionTypeEnum.labelOf(vo.getType()));

        // 票种明细（含停售的，编辑时四个票价输入框要能回填）
        List<TicketType> ticketTypes = ticketTypeMapper.listAllByExhibitionId(id);
        vo.setTickets(ticketTypes.stream()
                .map(t -> AdminExhibitionDetailVO.TicketItem.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .price(t.getPrice())
                        .totalStock(t.getTotalStock())
                        .soldCount(t.getSoldCount())
                        .limitPerUser(t.getLimitPerUser())
                        .status(t.getStatus())
                        .build())
                .collect(Collectors.toList()));

        vo.setCalendarCount(exhibitionMapper.countCalendar(id));
        return vo;
    }

    // ==================================================================
    // 新增
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminExhibitionCreateVO create(AdminExhibitionSaveDTO dto) {
        // 1. 基本校验：展期合法
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        // 2. 校验展馆存在，并拿到城市（用于生成编号）
        GalleryDetailVO gallery = galleryMapper.getGalleryById(dto.getGalleryId());
        if (gallery == null) {
            throw new BaseException(MessageConstant.GALLERY_NOT_FOUND);
        }

        // 3. 生成展览编号：城市前缀 + 3 位序号（NJ007）
        String exhibitionCode = generateExhibitionCode(gallery.getCity());

        // 4. 插展览
        AdminExhibitionDetailVO toInsert = AdminExhibitionDetailVO.builder()
                .exhibitionCode(exhibitionCode)
                .title(dto.getTitle())
                .galleryId(dto.getGalleryId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .type(dto.getType() == null ? 1 : dto.getType())
                .openTime(dto.getOpenTime())
                .dailyStockLimit(dto.getDailyStockLimit())
                .posterImage(dto.getPosterImage())
                .images(dto.getImages())
                .description(dto.getDescription())
                .isHot(dto.getIsHot() == null ? 0 : dto.getIsHot())
                .sortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .build();
        exhibitionMapper.insertExhibition(toInsert);
        Long exhibitionId = toInsert.getId();

        // 5. 插票种（只建传了价格的），并顺手补 exhibition 的展示价
        int days = (int) (ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1);
        int ticketCount = insertTicketTypes(exhibitionId, dto, days);

        // 6. 🔑 按展期批量生成日历库存：每天一条，total_stock = 每日票数上限
        List<LocalDate> dates = buildDateRange(dto.getStartDate(), dto.getEndDate());
        exhibitionCalendarMapper.batchInsertCalendar(exhibitionId, dates, dto.getDailyStockLimit());

        // 7. 同步美术馆的在展数量（该展览若在展期内就要 +1）
        syncGalleryExhibitionCount(dto.getGalleryId());

        log.info("新增展览成功，id：{}，编号：{}，票种：{}，生成日历：{} 天",
                exhibitionId, exhibitionCode, ticketCount, dates.size());

        return AdminExhibitionCreateVO.builder()
                .id(exhibitionId)
                .exhibitionCode(exhibitionCode)
                .calendarGenerated(dates.size())
                .ticketTypeCount(ticketCount)
                .build();
    }

    /**
     * 插入票种，并回写 exhibition 的展示价（取最低价）与总库存
     *
     * @param days 展期天数，用于算票种默认总库存
     * @return 实际插入的票种数
     */
    private int insertTicketTypes(Long exhibitionId, AdminExhibitionSaveDTO dto, int days) {
        if (dto.getTickets() == null || dto.getTickets().isEmpty()) {
            return 0;
        }

        int count = 0;
        for (AdminExhibitionSaveDTO.TicketItem item : dto.getTickets()) {
            if (item == null || item.getName() == null || item.getName().trim().isEmpty()) {
                continue;
            }
            // 默认总库存 = 每日票数上限 × 展期天数（保证整场展览的票够卖）
            int totalStock = item.getTotalStock() != null && item.getTotalStock() > 0
                    ? item.getTotalStock()
                    : dto.getDailyStockLimit() * days;

            TicketType ticketType = TicketType.builder()
                    .exhibitionId(exhibitionId)
                    .name(item.getName().trim())
                    .price(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice())
                    .totalStock(totalStock)
                    .limitPerUser(item.getLimitPerUser() == null ? 5 : item.getLimitPerUser())
                    .sort(item.getSort() == null ? 0 : item.getSort())
                    .status(0)
                    .build();
            ticketTypeMapper.insertTicketType(ticketType);
            count++;
        }

        // 与编辑路径共用同一口径，避免两条路算出的展示价/库存不一致
        if (count > 0) {
            syncDisplayPriceAndStock(exhibitionId);
        }
        return count;
    }

    // ==================================================================
    // 编辑
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminExhibitionSaveDTO dto) {
        AdminExhibitionDetailVO exist = exhibitionMapper.getAdminExhibitionById(id);
        if (exist == null) {
            throw new BaseException(MessageConstant.EXHIBITION_NOT_FOUND);
        }
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        GalleryDetailVO gallery = galleryMapper.getGalleryById(dto.getGalleryId());
        if (gallery == null) {
            throw new BaseException(MessageConstant.GALLERY_NOT_FOUND);
        }

        AdminExhibitionDetailVO toUpdate = AdminExhibitionDetailVO.builder()
                .id(id)
                .title(dto.getTitle())
                .galleryId(dto.getGalleryId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .type(dto.getType() == null ? exist.getType() : dto.getType())
                .openTime(dto.getOpenTime())
                .dailyStockLimit(dto.getDailyStockLimit())
                .posterImage(dto.getPosterImage())
                .images(dto.getImages())
                .description(dto.getDescription())
                .isHot(dto.getIsHot() == null ? exist.getIsHot() : dto.getIsHot())
                .sortOrder(dto.getSortOrder() == null ? exist.getSortOrder() : dto.getSortOrder())
                .build();
        exhibitionMapper.updateExhibition(toUpdate);

        // 展期变了：处理日历
        if (dto.getEndDate().isBefore(exist.getEndDate())) {
            // 展期缩短 —— 超出新闭展日的日历行要下线
            // ⚠️ 未售出的可以删，已售出的只能置 status=1（有订单引用，不能删）
            exhibitionCalendarMapper.closeCalendarAfter(id, dto.getEndDate());
            exhibitionCalendarMapper.deleteUnsoldCalendarAfter(id, dto.getEndDate());
        } else if (dto.getEndDate().isAfter(exist.getEndDate())) {
            // 展期延长 —— 补生成新增天数的日历行（只补还没有的日期）
            List<LocalDate> all = buildDateRange(dto.getStartDate(), dto.getEndDate());
            List<LocalDate> existDates = exhibitionCalendarMapper.listAllDates(id);
            List<LocalDate> toAdd = all.stream()
                    .filter(d -> !existDates.contains(d))
                    .collect(Collectors.toList());
            if (!toAdd.isEmpty()) {
                exhibitionCalendarMapper.batchInsertCalendar(id, toAdd, dto.getDailyStockLimit());
            }
        }

        // 每日票数上限调整：把今天及以后的库存统一改过去
        exhibitionCalendarMapper.updateDailyStock(id, dto.getDailyStockLimit());

        // 票种：传了就整体重建（未售出的删、已售出的停售）
        if (dto.getTickets() != null) {
            rebuildTicketTypes(id, dto);
        }

        syncGalleryExhibitionCount(dto.getGalleryId());
    }

    /**
     * 重建票种：以传进来的为准
     * <ul>
     *     <li>已被订单引用的票种 → 只停售，不删</li>
     *     <li>没被引用的旧票种 → 物理删除</li>
     *     <li>传进来的 → 已有则更新，没有则新增</li>
     * </ul>
     */
    private void rebuildTicketTypes(Long exhibitionId, AdminExhibitionSaveDTO dto) {
        List<TicketType> old = ticketTypeMapper.listAllByExhibitionId(exhibitionId);
        Map<String, TicketType> oldByName = old.stream()
                .collect(Collectors.toMap(TicketType::getName, t -> t, (a, b) -> a));
        List<String> keepNames = dto.getTickets().stream()
                .map(AdminExhibitionSaveDTO.TicketItem::getName)
                .collect(Collectors.toList());

        // 处理旧票种：没在新列表里的
        for (TicketType t : old) {
            if (keepNames.contains(t.getName())) {
                continue;
            }
            if (ticketTypeMapper.countOrdersByTicketType(t.getId()) > 0) {
                ticketTypeMapper.disableTicketType(t.getId());   // 有订单 → 停售
            } else {
                ticketTypeMapper.deleteTicketType(t.getId());     // 无订单 → 删除
            }
        }

        // 处理新列表：更新或新增
        int days = (int) (ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1);
        for (AdminExhibitionSaveDTO.TicketItem item : dto.getTickets()) {
            if (item.getName() == null || item.getName().trim().isEmpty()) {
                continue;
            }
            TicketType existTicket = oldByName.get(item.getName());
            int totalStock = item.getTotalStock() != null && item.getTotalStock() > 0
                    ? item.getTotalStock()
                    : dto.getDailyStockLimit() * days;
            if (existTicket != null) {
                existTicket.setPrice(item.getPrice() == null ? existTicket.getPrice() : item.getPrice());
                existTicket.setTotalStock(totalStock);
                existTicket.setLimitPerUser(item.getLimitPerUser() == null ? 5 : item.getLimitPerUser());
                existTicket.setSort(item.getSort() == null ? existTicket.getSort() : item.getSort());
                // updateTicketType 里带 total_stock >= sold_count 条件，防止改成小于已售
                ticketTypeMapper.updateTicketType(existTicket);
            } else {
                ticketTypeMapper.insertTicketType(TicketType.builder()
                        .exhibitionId(exhibitionId)
                        .name(item.getName().trim())
                        .price(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice())
                        .totalStock(totalStock)
                        .limitPerUser(item.getLimitPerUser() == null ? 5 : item.getLimitPerUser())
                        .sort(item.getSort() == null ? 0 : item.getSort())
                        .status(0)
                        .build());
            }
        }

        // 🔑 票种改完后必须回写 exhibition 的展示价与总库存，
        //    否则 C 端列表上的价格/库存会一直是新增时的旧值
        syncDisplayPriceAndStock(exhibitionId);
    }

    /**
     * 用当前**在售**票种重算并回写 exhibition 的展示价（最低价）与总库存（各票种库存合计）
     * <p>
     * 新建与编辑都要调，是两处唯一的口径来源。
     */
    private void syncDisplayPriceAndStock(Long exhibitionId) {
        // 用 listAllByExhibitionId（返回实体，含 totalStock/price）；
        // listByExhibitionId 返回的是 C 端 VO，只有 remaining 没有 totalStock，不能用在这里
        List<TicketType> current = ticketTypeMapper.listAllByExhibitionId(exhibitionId);
        if (current == null || current.isEmpty()) {
            return;
        }
        BigDecimal minPrice = null;
        int totalStockSum = 0;
        for (TicketType t : current) {
            if (t.getTotalStock() != null) {
                totalStockSum += t.getTotalStock();
            }
            if (t.getPrice() != null && (minPrice == null || t.getPrice().compareTo(minPrice) < 0)) {
                minPrice = t.getPrice();
            }
        }
        exhibitionMapper.updateDisplayPriceAndStock(exhibitionId,
                minPrice == null ? BigDecimal.ZERO : minPrice, totalStockSum);
    }

    // ==================================================================
    // 删除 / 状态 / 库存
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AdminExhibitionDetailVO exist = exhibitionMapper.getAdminExhibitionById(id);
        if (exist == null) {
            throw new BaseException(MessageConstant.EXHIBITION_NOT_FOUND);
        }
        // 🔑 有有效订单的展览不能删：订单是财务数据，删了展览会变成「孤儿订单」
        int activeOrders = exhibitionMapper.countActiveOrders(id);
        if (activeOrders > 0) {
            throw new BaseException("该展览已有 " + activeOrders + " 笔订单，不能删除，请改为下架");
        }

        // 没有订单：连带清掉票种与日历，避免留下孤儿数据
        List<TicketType> tickets = ticketTypeMapper.listAllByExhibitionId(id);
        for (TicketType t : tickets) {
            ticketTypeMapper.deleteTicketType(t.getId());
        }
        exhibitionCalendarMapper.deleteAllByExhibitionId(id);
        exhibitionMapper.deleteExhibition(id);

        syncGalleryExhibitionCount(exist.getGalleryId());
        log.info("删除展览成功，id：{}，编号：{}", id, exist.getExhibitionCode());
    }

    @Override
    public void updateStatus(Long id, AdminExhibitionStatusDTO dto) {
        if (exhibitionMapper.getAdminExhibitionById(id) == null) {
            throw new BaseException(MessageConstant.EXHIBITION_NOT_FOUND);
        }
        // 三个字段全空会导致 <set> 生成非法 SQL
        if (dto.getStatus() == null && dto.getIsHot() == null && dto.getSortOrder() == null) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }
        exhibitionMapper.updateStatus(id, dto.getStatus(), dto.getIsHot(), dto.getSortOrder());
    }

    @Override
    public void updateStock(Long id, AdminExhibitionStockDTO dto) {
        AdminExhibitionDetailVO exist = exhibitionMapper.getAdminExhibitionById(id);
        if (exist == null) {
            throw new BaseException(MessageConstant.EXHIBITION_NOT_FOUND);
        }
        boolean changed = false;

        // 用法一：改每日票数上限（今天及以后）
        if (dto.getDailyStockLimit() != null) {
            exhibitionCalendarMapper.updateDailyStock(id, dto.getDailyStockLimit());
            // 同步展览表上的上限字段，后台列表要展示
            exhibitionMapper.updateDailyStockLimit(id, dto.getDailyStockLimit());
            changed = true;
        }

        // 用法二：精确调整某几天
        if (dto.getCalendarAdjust() != null && !dto.getCalendarAdjust().isEmpty()) {
            for (AdminExhibitionStockDTO.CalendarAdjust item : dto.getCalendarAdjust()) {
                int rows = exhibitionCalendarMapper.updateStockByDate(id, item.getDate(), item.getTotalStock());
                if (rows == 0) {
                    // 两种可能：该日不在日历里，或新库存小于已售数量
                    throw new BaseException("日期 " + item.getDate()
                            + " 调整失败：该日未放票，或新库存小于已售数量");
                }
            }
            changed = true;
        }

        if (!changed) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }
    }

    // ==================================================================
    // 私有工具方法
    // ==================================================================

    /** 展期校验：闭展不能早于开展 */
    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BaseException("请填写开展与闭展日期");
        }
        if (end.isBefore(start)) {
            throw new BaseException("闭展日期不能早于开展日期");
        }
    }

    /** 展期内每一天（含首尾） */
    private List<LocalDate> buildDateRange(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate d = start;
        while (!d.isAfter(end)) {
            dates.add(d);
            d = d.plusDays(1);
        }
        return dates;
    }

    /**
     * 生成展览编号：城市前缀 + 3 位序号，如 NJ007
     * <p>
     * 取该城市已有编号的最大序号 +1；没有任何编号时从 1 开始
     */
    private String generateExhibitionCode(String city) {
        String prefix = CITY_CODE_PREFIX.getOrDefault(city, DEFAULT_CITY_PREFIX);
        Integer maxSeq = exhibitionMapper.getMaxCodeSeqOfCity(city);
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%03d", next);
    }

    /** 同步美术馆的「当前在展数量」冗余字段 */
    private void syncGalleryExhibitionCount(Long galleryId) {
        if (galleryId == null) {
            return;
        }
        galleryMapper.syncExhibitionCount(galleryId);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
