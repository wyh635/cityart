package com.cityart.interceptor;

import com.cityart.constant.JwtClaimsConstant;
import com.cityart.context.AdminContext;
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
 * JWT 校验拦截器（B 端管理员）
 * <p>
 * 与 C 端的 {@link JwtTokenUserInterceptor} 是**两套独立体系**，三处必须不同：
 * <table border="1">
 *     <tr><th></th><th>C 端</th><th>B 端（本类）</th></tr>
 *     <tr><td>拦截路径</td><td>/api/app/**</td><td>/api/admin/**</td></tr>
 *     <tr><td>签名密钥</td><td>user-secret-key</td><td>admin-secret-key</td></tr>
 *     <tr><td>claims 键</td><td>userId</td><td>adminId</td></tr>
 *     <tr><td>ThreadLocal</td><td>BaseContext</td><td>AdminContext</td></tr>
 * </table>
 * 两套密钥不同，所以 C 端 token 拿到 B 端接口也验不过（返回 401）。
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

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

        // 1. 取令牌：请求头名由 yml 的 cityart.jwt.admin-token-name 决定，当前为 token
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        // 2. 校验令牌（注意：不要把 token 本身打进日志）
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long adminId = Long.valueOf(claims.get(JwtClaimsConstant.ADMIN_ID).toString());
            log.info("当前管理员id：{}", adminId);

            // 3. 存入 B 端专属 ThreadLocal（不是 BaseContext！）
            AdminContext.setCurrentId(adminId);
            return true;
        } catch (Exception ex) {
            // 令牌缺失、被篡改、已过期，或用的是 C 端密钥签的：一律 401
            response.setStatus(401);
            return false;
        }
    }

    /**
     * 请求结束后清理 ThreadLocal（必须做，否则线程复用会串管理员身份）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        AdminContext.removeCurrentId();
    }
}
