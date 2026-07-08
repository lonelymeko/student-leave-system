package com.school.leave.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours:168}")
    private long expireHours;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireHours * 3600_000L))
                .signWith(key)
                .compact();
    }

    /** 微信绑定临时票据：10 分钟有效，purpose=wx-bind，防止 openid 明文下发 */
    public String createBindTicket(String openid) {
        Date now = new Date();
        return Jwts.builder()
                .subject(openid)
                .claim("purpose", "wx-bind")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 10 * 60_000L))
                .signWith(key)
                .compact();
    }

    /** 解析绑定票据，返回 openid；无效/过期/用途不符返回 null */
    public String parseBindTicket(String ticket) {
        try {
            var claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(ticket).getPayload();
            if (!"wx-bind".equals(claims.get("purpose", String.class))) return null;
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析 token，失败返回 null */
    public Long parseUserId(String token) {
        try {
            String sub = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return Long.valueOf(sub);
        } catch (Exception e) {
            return null;
        }
    }
}
