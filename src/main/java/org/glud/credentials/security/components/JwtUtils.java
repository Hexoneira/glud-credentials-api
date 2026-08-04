package org.glud.credentials.security.components;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.glud.credentials.auth.model.Rol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationTime;

    @PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured. Set jwt.secret before starting the application.");
        }
        try {
            Decoders.BASE64.decode(jwtSecret);
        } catch (DecodingException e) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64 string of at least 32 bytes", e);
        }
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        LOGGER.error("JWT token is missing or does not start with Bearer");
        return null;
    }

    public String generateJwtToken(Long userId, Long tenantId, Rol roleId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId)
                .claim("roleId", roleId)
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime() + jwtExpirationTime)))
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            LOGGER.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromJwtToken(String jwt) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(jwt).getPayload().getSubject();
    }
}


