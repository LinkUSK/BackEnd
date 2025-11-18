package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // application.properties 와 맞춤 (jwt.secret / jwt.expire-ms)
    @Value("${jwt.secret:mySuperSecretKeyForJwtTokenShouldBeLongEnough!}")
    private String secret;

    @Value("${jwt.expire-ms:2592000000}") // 30일
    private long validityMs;

    private Key signingKey() {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) raw = Arrays.copyOf(raw, 32); // HS256: 32바이트 이상
        return Keys.hmacShaKeyFor(raw);
    }

    /** 토큰 생성 (subject 에 userId/username 저장) */
    public String generateToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(validityMs)))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 유효성 검사 */
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** subject 문자열(userId/이메일/숫자PK 등) */
    public String getUsername(String token) {
        Claims c = Jwts.parserBuilder().setSigningKey(signingKey()).build()
                .parseClaimsJws(token).getBody();
        return c.getSubject();
    }

    /** subject가 숫자면 Long으로 */
    public Long getUserIdAsLong(String token) {
        try { String sub = getUsername(token); return (sub==null)?null:Long.valueOf(sub); }
        catch (Exception e) { return null; }
    }

    /* ====== 레거시 이름 호환 ====== */
    public boolean validateToken(String token){ return validate(token); }
    public Long getUserIdFromToken(String token){ return getUserIdAsLong(token); }
    public String getUserIdFromTokenAsString(String token){ return getUsername(token); }
}
