package com.cityart.interceptor;

import com.cityart.annotation.OptionalLogin;
import com.cityart.constant.JwtClaimsConstant;
import com.cityart.context.BaseContext;
import com.cityart.properties.JwtProperties;
import com.cityart.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 校验拦截器（C 端用户）
 * <p>
 * 职责：从请求头取 token → 验签 → 把 userId 写入 BaseContext（ThreadLocal），
 * 请求结束后清理，避免 Tomcat 线程复用导致用户身份串号。
 * <p>
 * 两种模式：
 * <ul>
 *     <li><b>必须登录</b>（默认）：无 token / token 无效 → 401</li>
 *     <li><b>可选登录</b>（方法上标了 {@link OptionalLogin}）：无 token / token 无效 → 放行，
 *         此时 {@code BaseContext.getCurrentId()} 为 null，业务层按游客处理</li>
 * </ul>
 * 详情页就是可选登录：游客能看，登录后额外返回 {@code isFavorited} 真实收藏状态。
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 目标方法执行前校验 token
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只拦截 Controller 方法（放行静态资源等）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1. 从请求头取出令牌（请求头名由 yml 的 cityart.jwt.user-token-name 决定，当前为 authentication）
        String token = request.getHeader(jwtProperties.getUserTokenName());

        // 2. 是否「可选登录」接口（注解写在方法上）
        OptionalLogin optionalLogin = ((HandlerMethod) handler).getMethodAnnotation(OptionalLogin.class);

        // 3. 没带 token
        if (token == null || token.trim().isEmpty()) {
            if (optionalLogin != null) {
                // 可选登录：按游客放行，userId 保持为 null
                log.info("可选登录接口未携带 token，按游客处理：{}", request.getRequestURI());
                return true;
            }
            response.setStatus(401);
            return false;
        }

        // 4. 带了 token 就验签（注意：不要把 token 本身打进日志）
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户id：{}", userId);

            // 5. 存入 ThreadLocal，后续 Controller/Service 用 BaseContext.getCurrentId() 取
            BaseContext.setCurrentId(userId);
            return true;
        } catch (Exception ex) {
            // 可选登录：token 过期/被篡改也不报错，降级为游客；其余接口返回 401
            if (optionalLogin != null) {
                log.warn("可选登录接口 token 无效，按游客处理：{}", request.getRequestURI());
                return true;
            }
            response.setStatus(401);
            return false;
        }
    }

    /**
     * 请求结束后清理 ThreadLocal（必须做，否则线程复用会串数据）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
