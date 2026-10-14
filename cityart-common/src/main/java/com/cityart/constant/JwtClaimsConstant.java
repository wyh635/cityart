package com.cityart.constant;

/**
 * JWT 载荷（claims）的键名
 * <p>
 * 签发时用这些键往 claims 里放值，验签时用同样的键取出来。
 * 键名写错不会报错，只会取到 null —— 所以统一在这里定义，不散落魔法字符串。
 */
public class JwtClaimsConstant {

    // ==================== B 端管理员 ====================

    /** 管理员 id（B 端 token 用这个键） */
    public static final String ADMIN_ID = "adminId";

    /** 管理员姓名 */
    public static final String NAME = "name";

    // ==================== C 端用户 ====================

    /** 用户 id（C 端 token 用这个键） */
    public static final String USER_ID = "userId";

    /** 用户手机号 */
    public static final String PHONE = "phone";

    /** 用户名（预留） */
    public static final String USERNAME = "username";

    /** 员工 id（从苍穹外卖模板带过来的，本项目未使用） */
    public static final String EMP_ID = "empId";

}
