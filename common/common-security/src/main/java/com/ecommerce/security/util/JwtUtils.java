package com.ecommerce.security.util;

import com.ecommerce.core.constant.GlobalConstants;
import com.ecommerce.core.model.TokenPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类
 * <p>
 * 提供 Token 生成、解析和验证功能，使用 HS256 算法进行签名。
 * <p>
 * <b>安全提醒：</b>当前密钥硬编码仅用于开发环境。生产环境必须：
 * <ul>
 *   <li>从 Nacos 配置中心或环境变量读取密钥（如 @Value("${jwt.secret}")）</li>
 *   <li>密钥长度 ≥256 位（32 字节），不同环境使用不同密钥</li>
 *   <li>定期轮换密钥（建议通过 Nacos 动态配置实现热更新）</li>
 * </ul>
 * <p>
 * Token 结构（JWT Claims）：
 * <ul>
 *   <li>sub — 用户 ID</li>
 *   <li>username / nickname — 用户标识</li>
 *   <li>roles — 角色列表（Gateway 鉴权用）</li>
 *   <li>iat / exp — 签发时间 / 过期时间（默认 7200 秒）</li>
 * </ul>
 */
@Slf4j
public class JwtUtils {

    // ⚠️ 仅用于开发环境，生产环境必须从 Nacos 配置中心或环境变量读取
    // 建议：@Value("${jwt.secret}") 配合 Nacos 动态配置实现密钥轮换
    private static final String SECRET = "ecommerce-platform-jwt-secret-key-2024-min-256-bits!!";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8));

    /**
 * 根据 TokenPayload 生成 JWT 字符串。
 * <p>
 * Token 包含用户身份信息（userId、username、roles、merchantId 等），
 * 过期时间由 {@link GlobalConstants#TOKEN_EXPIRE_SECONDS} 控制（默认 7200 秒）。
 *
 * @param payload 用户身份信息
 * @return 签发的 JWT 字符串
 */
public static String generateToken(TokenPayload payload) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
            .subject(String.valueOf(payload.getUserId()))
            .claim("username", payload.getUsername())
            .claim("nickname", payload.getNickname())
            .claim("roles", payload.getRoles())
            .claim("merchantId", payload.getMerchantId())
            .issuedAt(new Date(now))
            .expiration(new Date(now + GlobalConstants.TOKEN_EXPIRE_SECONDS * 1000))
            .signWith(SECRET_KEY)
            .compact();
}

/**
 * 解析 JWT 字符串，提取 TokenPayload。
 * <p>
 * 解析失败（过期、签名不匹配、格式错误等）返回 null，不抛异常，
 * 调用方通过 null 判断即可。
 *
 * @param token JWT 字符串（不含 Bearer 前缀）
 * @return 解析后的 TokenPayload，失败返回 null
 */
public static TokenPayload parseToken(String token) {
    try {
        Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        TokenPayload payload = new TokenPayload();
        payload.setUserId(Long.parseLong(claims.getSubject()));
        payload.setUsername(claims.get("username", String.class));
        payload.setNickname(claims.get("nickname", String.class));
        payload.setRoles(claims.get("roles", List.class));
        payload.setMerchantId(claims.get("merchantId", Long.class));
        payload.setIat(claims.getIssuedAt().getTime());
        payload.setExp(claims.getExpiration().getTime());
        return payload;
    } catch (Exception e) {
        log.warn("JWT解析失败: {}", e.getMessage());
        return null;
    }
}

    public static boolean validateToken(String token) {
        return parseToken(token) != null;
    }
}
