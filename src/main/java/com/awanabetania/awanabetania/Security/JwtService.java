package com.awanabetania.awanabetania.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Issues and validates the JSON Web Tokens used to authenticate API requests.
 * <p>
 * The token carries the caller's identity ({@code sub}), their account type
 * ({@code kind}: CHILD or LEADER), their database id and their role. Because the
 * token is signed server-side with {@code jwt.secret}, the client cannot alter any
 * of these values — which is what makes the previous localStorage-based role check
 * safe to replace.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long validityMillis;

    /**
     * @param secret         HMAC signing secret; must be at least 32 characters
     * @param validityHours  how long an issued token stays valid
     */
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.validity-hours:12}") long validityHours) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters (got " + bytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.validityMillis = validityHours * 60 * 60 * 1000;
    }

    /**
     * Builds a signed token describing an authenticated account.
     *
     * @param username the account's unique username, stored as the subject
     * @param kind     {@code CHILD} or {@code LEADER}
     * @param id       the account's database id
     * @param role     the account's role, or {@code null} for children
     * @return a compact, signed JWT
     */
    public String issue(String username, String kind, Integer id, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "kind", kind,
                        "uid", id == null ? -1 : id,
                        "role", role == null ? "" : role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMillis))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies a token's signature and expiry and returns its claims.
     *
     * @param token the compact JWT taken from the Authorization header
     * @return the parsed claims, or {@code null} if the token is invalid or expired
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
