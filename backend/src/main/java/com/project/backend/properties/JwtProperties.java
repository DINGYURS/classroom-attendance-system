package com.project.backend.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "rollcallsystem.jwt")
public class JwtProperties {

    /**
     * 管理端签名密钥
     */
    private String adminSecretKey;

    /**
     * 管理端过期时间（毫秒）
     */
    private Long adminTtl;

    /**
     * 管理端令牌名称
     */
    private String adminTokenName;
}
