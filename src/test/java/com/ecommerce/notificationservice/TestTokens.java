package com.ecommerce.notificationservice;

import io.jsonwebtoken.Jwts;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Makes access tokens like user-service does, for tests. */
public final class TestTokens {

    // Fake value for tests only.
    public static final String SECRET = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"; // gitleaks:allow

    private TestTokens() {
    }

    public static String token(long userId, String roles, long ttlMillis) {
        return token(SECRET, userId, roles, ttlMillis);
    }

    public static String token(String secret, long userId, String roles, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim("roles", roles)
                .claim("name", "Nimal Perera")
                .claim("email", "nimal@example.com")
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .compact();
    }
}
