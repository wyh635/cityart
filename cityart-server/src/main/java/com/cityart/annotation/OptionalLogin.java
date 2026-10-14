package com.cityart.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 可选登录标记
 * <p>
 * 用在「游客能看、登录后能看个性化字段」的接口上（如详情页的 {@code isFavorited} 收藏状态）。
 * <p>
 * 拦截器行为：
 * <ul>
 *     <li>不带 token → 放行，{@code BaseContext.getCurrentId()} 为 null（按游客处理）</li>
 *     <li>带 token   → 照常验签并写入 userId</li>
 * </ul>
 * 不加本注解的接口一律要求 token，无 token 返回 401。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalLogin {
}
