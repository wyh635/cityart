package com.cityart.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果，统一返回给前端
 * <pre>
 * {
 *   "total": 23,     总记录数
 *   "pages": 3,      总页数
 *   "page": 1,       当前页码
 *   "pageSize": 10,  每页条数
 *   "list": []       当前页数据
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Integer pages;

    /** 当前页码 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 当前页数据 */
    private List<T> list;

    /**
     * 组装分页结果，总页数在这里算，Service 不用自己算
     *
     * @param total    总记录数（count 查询的结果）
     * @param page     当前页码
     * @param pageSize 每页条数
     * @param list     当前页数据
     */
    public static <T> PageResult<T> of(long total, int page, int pageSize, List<T> list) {
        // 向上取整：23 条、每页 10 条 → 3 页
        int pages = pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
        return PageResult.<T>builder()
                .total(total)
                .pages(pages)
                .page(page)
                .pageSize(pageSize)
                .list(list)
                .build();
    }
}
