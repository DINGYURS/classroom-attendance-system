package com.project.backend.utils;

import com.project.backend.constant.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
public class JwtUtils {

    /**
     * 生成 JWT 令牌
     *
     * @param secretKey 签名密钥
     * @param ttlMillis 过期时间（毫秒）
     * @param claims    自定义声明
     * @return JWT 令牌
     */
    public static String createJwt(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        return Jwts.builder()
                .claims(claims)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 JWT 令牌
     *
     * @param secretKey 签名密钥
     * @param token     JWT 令牌
     * @return Claims 声明
     */
    public static Claims parseJwt(String secretKey, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Claims 中获取用户 ID
     */
    public static Long getUserId(Claims claims) {
        return claims.get(JwtConstants.CLAIMS_USER_ID, Long.class);
    }

    /**
     * 从 Claims 中获取用户名
     */
    public static String getUsername(Claims claims) {
        return claims.get(JwtConstants.CLAIMS_USERNAME, String.class);
    }

    /**
     * 从 Claims 中获取角色
     */
    public static Integer getRole(Claims claims) {
        return claims.get(JwtConstants.CLAIMS_ROLE, Integer.class);
    }
}
