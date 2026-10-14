package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 票夹分页结果：比通用分页多一个 validCount（右上角「共 N 张有效门票」）
 */
@Data
@ApiModel("票夹分页VO")
public class TicketPageVO implements Serializable {

    private Long total;

    private Integer pages;

    private Integer page;

    private Integer pageSize;

    @ApiModelProperty("有效门票数量（右上角「共 N 张有效门票」）")
    private Integer validCount;

    private List<TicketVO> list;

    public static TicketPageVO of(long total, int page, int pageSize, List<TicketVO> list, int validCount) {
        TicketPageVO vo = new TicketPageVO();
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        // 向上取整：总数 5、每页 1 张 → 5 页
        vo.setPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize));
        vo.setList(list);
        vo.setValidCount(validCount);
        return vo;
    }
}
