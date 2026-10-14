package com.cityart.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（C 端用户 + B 端管理员两套）
 * <p>
 * 绑定 yml 的 {@code cityart.jwt}，kebab-case 自动映射到小驼峰：
 * <pre>
 * cityart:
 *   jwt:
 *     user-secret-key: cityart-user      → userSecretKey
 *     user-ttl: 7200000                  → userTtl
 *     user-token-name: authentication    → userTokenName
 *     admin-secret-key: cityart-admin    → adminSecretKey
 *     admin-ttl: 7200000                 → adminTtl
 *     admin-token-name: token            → adminTokenName
 * </pre>
 * 两套密钥必须不同，否则 C 端 token 能直接访问 B 端接口。
 */
@Component
@ConfigurationProperties(prefix = "cityart.jwt")
@Data
public class JwtProperties {

    // ==================== C 端用户 ====================

    /** 用户端 jwt 签名密钥 */
    private String userSecretKey;

    /** 用户端 jwt 过期时间（毫秒） */
    private long userTtl;

    /** 用户端前端传递过来的令牌名称（请求头名） */
    private String userTokenName;

    // ==================== B 端管理员 ====================

    /** 管理端 jwt 签名密钥 */
    private String adminSecretKey;

    /** 管理端 jwt 过期时间（毫秒） */
    private long adminTtl;

    /** 管理端前端传递过来的令牌名称（请求头名） */
    private String adminTokenName;

}
