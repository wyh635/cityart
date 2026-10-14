package com.cityart.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 美术馆分类字典
 * <p>
 * 与 {@link ExhibitionTypeEnum} 同理：中文名由后端字典接口维护，前端不硬编码。
 * 「全部类型」不占值，前端不传 type 参数即可。
 */
@Getter
@AllArgsConstructor
public enum GalleryTypeEnum {

    COMPREHENSIVE(1, "综合美术馆"),
    CONTEMPORARY(2, "当代美术馆"),
    CLASSICAL(3, "古典美术馆"),
    SCULPTURE(4, "雕塑美术馆"),
    PHOTOGRAPHY(5, "摄影美术馆");

    /** 传给列表接口 type 参数的值 */
    private final Integer value;

    /** 下拉框显示的中文名 */
    private final String label;

    /**
     * 根据类型值取中文名，取不到返回 null
     */
    public static String labelOf(Integer value) {
        if (value == null) {
            return null;
        }
        for (GalleryTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item.label;
            }
        }
        return null;
    }
}
