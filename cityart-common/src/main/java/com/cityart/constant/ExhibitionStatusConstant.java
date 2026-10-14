package com.cityart.constant;

/**
 * 展览展出状态（不落库，由 start_date / end_date 与今天实时比较得出）
 */
public class ExhibitionStatusConstant {

    /** 正在展出：start_date <= 今天 <= end_date */
    public static final String CURRENT = "current";

    /** 即将展出：start_date > 今天 */
    public static final String FUTURE = "future";


    /** 往期展出：end_date < 今天 */
    public static final String PAST = "past";
}
