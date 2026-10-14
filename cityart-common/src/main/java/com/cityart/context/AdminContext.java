package com.cityart.context;

/**
 * B 端管理员上下文（ThreadLocal 存当前登录管理员 id）
 * <p>
 * ⚠️ <b>为什么单独一个类，不复用 {@link BaseContext}？</b>
 * <p>
 * C 端用户和 B 端管理员是两套完全独立的身份体系：
 * <ul>
 *     <li>同一个 JVM 线程池会被两种请求复用</li>
 *     <li>如果共用同一个 ThreadLocal，B 端请求写入的 adminId 会被 C 端接口读到（反之亦然）</li>
 *     <li>后果：C 端接口拿管理员 id 当 userId 去查数据 → 越权或查到错误数据</li>
 * </ul>
 * 所以两个身份各用一个 ThreadLocal，物理隔离。
 * <p>
 * 用法与 {@link BaseContext} 完全一致：拦截器 preHandle 写入，afterCompletion 清理。
 */
public class AdminContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /** 写入当前登录管理员 id */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /** 取当前登录管理员 id；未登录（或已清理）返回 null */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /** 清理，必须在 afterCompletion 调用，否则线程复用会串身份 */
    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
