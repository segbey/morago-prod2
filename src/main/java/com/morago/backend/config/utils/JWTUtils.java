package com.morago.backend.config.utils;

import com.morago.backend.entity.enumFiles.TokenType;
import com.morago.backend.exception.token.ExpireJwtTokenException;
import com.morago.backend.exception.token.InvalidJwtTokenException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JWTUtils {
    private static final long CLOCK_SKEW_SEC = 60;

    private final JWTProperties jwtProperties;
    public JWTUtils(JWTProperties jwtProperties) { this.jwtProperties = jwtProperties; }

    private Key getAccessSigningKey() {
        byte[] key = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtProperties.getAccessSecret());
        return Keys.hmacShaKeyFor(key);
    }
    private Key getRefreshSigningKey() {
        byte[] key = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtProperties.getRefreshSecret());
        return Keys.hmacShaKeyFor(key);
    }
    private Key getSigningKey(TokenType type) {
        return (type == TokenType.ACCESS) ? getAccessSigningKey() : getRefreshSigningKey();
    }

    @jakarta.annotation.PostConstruct
    void validateConfig() {
        requireNonEmpty(jwtProperties.getAccessSecret(), "accessSecret");
        requireNonEmpty(jwtProperties.getRefreshSecret(), "refreshSecret");

        getAccessSigningKey();
        getRefreshSigningKey();
        if (jwtProperties.getAccessExpirationMs() <= 0 || jwtProperties.getRefreshExpirationMs() <= 0) {
            throw new IllegalStateException("JWT expiration must be > 0");
        }
    }
    private static void requireNonEmpty(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing JWT property: " + name);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtProperties.getAccessExpirationMs());

        return Jwts.builder()
                .setIssuer("morago")
                .setId(java.util.UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getAccessSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());

        return Jwts.builder()
                .setIssuer("morago")
                .setId(java.util.UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getRefreshSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token, TokenType type) {
        return parse(token, type).getSubject();
    }

    public Set<String> getRolesFromToken(String token, TokenType type) {
        Object raw = parse(token, type).get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().filter(String.class::isInstance).map(String.class::cast).collect(Collectors.toSet());
        }
        return Set.of();
    }

    public void validateToken(String token, TokenType type) {
        parse(token, type);
    }

    private io.jsonwebtoken.Claims parse(String token, TokenType type) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey(type))
                    .setAllowedClockSkewSeconds(CLOCK_SKEW_SEC)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ExpireJwtTokenException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtTokenException();
        }
    }

    public static String stripBearer(String token) {
        if (token == null) return null;
        token = token.trim();
        return token.regionMatches(true, 0, "Bearer ", 0, 7) ? token.substring(7).trim() : token;
    }
}
