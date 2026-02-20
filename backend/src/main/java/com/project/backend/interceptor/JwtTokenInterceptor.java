package com.project.backend.interceptor;

import com.project.backend.constant.JwtConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.properties.JwtProperties;
import com.project.backend.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 令牌校验拦截器
 */
@Slf4j
@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前拦截的是否为 Controller 方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 从请求头获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        // 令牌为空
        if (token == null || token.isEmpty()) {
            log.warn("请求头中缺少令牌");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 去除 Bearer 前缀
        if (token.startsWith(JwtConstants.TOKEN_PREFIX)) {
            token = token.substring(JwtConstants.TOKEN_PREFIX.length());
        }

        try {
            // 解析令牌
            Claims claims = JwtUtils.parseJwt(jwtProperties.getAdminSecretKey(), token);
            Long userId = JwtUtils.getUserId(claims);

            log.info("用户 ID: {}", userId);

            // 将用户 ID 存入上下文
            BaseContext.setCurrentId(userId);

            return true;
        } catch (Exception e) {
            log.warn("令牌解析失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除当前线程的用户信息
        BaseContext.remove();
    }
}
