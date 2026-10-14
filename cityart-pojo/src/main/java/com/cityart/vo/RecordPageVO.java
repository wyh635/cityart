package com.cityart.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 看展记录分页结果：多一个 totalVisitCount（「共参观 N 场展览」）
 */
@Data
@ApiModel("看展记录分页VO")
public class RecordPageVO implements Serializable {

    private Long total;

    private Integer pages;

    private Integer page;

    private Integer pageSize;

    @ApiModelProperty("累计参观次数（「共参观 N 场展览」）")
    private Integer totalVisitCount;

    private List<RecordVO> list;

    public static RecordPageVO of(long total, int page, int pageSize, List<RecordVO> list, int totalVisitCount) {
        RecordPageVO vo = new RecordPageVO();
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize));
        vo.setList(list);
        vo.setTotalVisitCount(totalVisitCount);
        return vo;
    }
}
