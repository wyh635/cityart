package com.cityart.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 展览分类字典
 * <p>
 * 中文名统一由后端维护（字典接口返回给前端），前端不要硬编码 1→综合展览 这类映射，
 * 以后改分类名只改这里一处。
 * <p>
 * ⚠️「全部类型」不占值：前端下拉第一项选「全部类型」时，不传 type 参数即可。
 */
@Getter
@AllArgsConstructor
public enum ExhibitionTypeEnum {

    COMPREHENSIVE(1, "综合展览"),
    CONTEMPORARY(2, "当代展览"),
    CLASSICAL(3, "古典展览"),
    SCULPTURE(4, "雕塑展览"),
    PHOTOGRAPHY(5, "摄影展览");

    /** 传给列表接口 type 参数的值 */
    private final Integer value;

    /** 下拉框显示的中文名 */
    private final String label;

    /**
     * 根据类型值取中文名，取不到返回 null（老数据 type 可能是 0 或空）
     */
    public static String labelOf(Integer value) {
        if (value == null) {
            return null;
        }
        for (ExhibitionTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item.label;
            }
        }
        return null;
    }
}
