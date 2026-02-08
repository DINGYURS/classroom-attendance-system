package com.project.backend.constant;

/**
 * JWT 相关常量
 */
public class JwtConstants {

    /**
     * JWT 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT 用户 ID Claims 键
     */
    public static final String CLAIMS_USER_ID = "userId";

    /**
     * JWT 用户名 Claims 键
     */
    public static final String CLAIMS_USERNAME = "username";

    /**
     * JWT 角色 Claims 键
     */
    public static final String CLAIMS_ROLE = "role";
}
