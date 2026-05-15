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
 * JWT工具类
 * 提供Token生成、解析和验证功能
 * 使用HS256算法进行签名，密钥长度至少256位
 */
@Slf4j
public class JwtUtils {

    // 仅用于开发环境，生产环境应从配置中心读取
    private static final String SECRET = "ecommerce-platform-jwt-secret-key-2024-min-256-bits!!";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(TokenPayload payload) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(payload.getUserId()))
                .claim("username", payload.getUsername())
                .claim("nickname", payload.getNickname())
                .claim("roles", payload.getRoles())
                .issuedAt(new Date(now))
                .expiration(new Date(now + GlobalConstants.TOKEN_EXPIRE_SECONDS * 1000))
                .signWith(SECRET_KEY)
                .compact();
    }

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
