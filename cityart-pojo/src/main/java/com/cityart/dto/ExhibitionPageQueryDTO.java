package com.cityart.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 「全部展览」列表分页查询参数
 * <p>
 * GET 请求的参数由 Spring MVC 自动绑定到这个对象上，
 * 校验注解写在字段上，Controller 方法参数加 @Validated 即生效（失败由全局异常处理器统一返回）。
 * <p>
 * offset（偏移量）不对外开放，由 page / pageSize 算出来给 SQL 的 LIMIT 用。
 */
@Data
@ApiModel("全部展览分页查询DTO")
public class ExhibitionPageQueryDTO implements Serializable {

    @Pattern(regexp = "current|future|past", message = "status 只能是 current / future / past")
    @ApiModelProperty("展出状态：current 正在展出 / future 即将展出 / past 往期展出，默认 current")
    private String status = "current";

    @Min(value = 1, message = "type 最小为 1")
    @Max(value = 5, message = "type 最大为 5")
    @ApiModelProperty("展览类型：1-5，不传表示全部类型")
    private Integer type;

    @Size(max = 50, message = "城市名称最多 50 个字符")
    @ApiModelProperty("城市，不传表示全部城市")
    private String city;

    @Size(max = 50, message = "搜索关键字最多 50 个字符")
    @ApiModelProperty("展览标题模糊搜索关键字")
    private String keyword;

    @Pattern(regexp = "default|priceAsc|priceDesc", message = "sort 只能是 default / priceAsc / priceDesc")
    @ApiModelProperty("排序：default 综合 / priceAsc 价格升序 / priceDesc 价格降序")
    private String sort = "default";

    @Min(value = 1, message = "page 最小为 1")
    @ApiModelProperty("页码，从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 50, message = "pageSize 最大为 50")
    @ApiModelProperty("每页条数，最大 50")
    private Integer pageSize = 10;

    /**
     * LIMIT 的偏移量 = (页码 - 1) × 每页条数
     * 只有 getter 没有 setter，前端传 offset 也会被忽略（防止绕过 pageSize 上限）
     */
    public int getOffset() {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        return (currentPage - 1) * size;
    }

    /**
     * 归一化后的每页条数，SQL 里直接用它
     */
    public int getLimit() {
        return (pageSize == null || pageSize < 1) ? 10 : pageSize;
    }
}
