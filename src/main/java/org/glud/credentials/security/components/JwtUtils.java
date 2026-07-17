package org.glud.credentials.security.components;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationTime;

    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        logger.error("JWT token is missing or does not start with Bearer");
        return null;
    }

    public String generateJwtToken(Long userId, Long tenantId, Long roleId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId)
                .claim("roleId", roleId)
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime() + jwtExpirationTime)))
                .signWith(key())
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try{
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            logger.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromJwtToken(String jwt) {
        return Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(jwt).getPayload().getSubject();
    }
}
