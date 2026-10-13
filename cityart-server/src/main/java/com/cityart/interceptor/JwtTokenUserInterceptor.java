package com.cityart.interceptor;

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

        // 2. 校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户id：{}", userId);

            // 3. 存入 ThreadLocal，后续 Controller/Service 用 BaseContext.getCurrentId() 取
            BaseContext.setCurrentId(userId);
            return true;
        } catch (Exception ex) {
            // 令牌缺失、被篡改或已过期：返回 401，前端据此跳登录页
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
