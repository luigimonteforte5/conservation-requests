package com.luigimonteforte.conservationrequests.security.service;

import com.luigimonteforte.conservationrequests.config.AppSecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final Duration jwtExpiration;

    public JwtService(AppSecurityProperties securityProperties) {
        secretKey = Keys.hmacShaKeyFor(securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        jwtExpiration = securityProperties.jwtExpiration();
    }

    public String generateToken(String username) {
        Instant actualTime = Instant.now();
        return Jwts.builder().subject(username).issuedAt(Date.from(actualTime)).expiration(Date.from(actualTime.plus(jwtExpiration))).signWith(secretKey).compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
