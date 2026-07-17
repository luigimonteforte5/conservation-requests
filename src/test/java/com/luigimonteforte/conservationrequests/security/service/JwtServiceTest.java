package com.luigimonteforte.conservationrequests.security.service;

import com.luigimonteforte.conservationrequests.config.AppSecurityProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JwtServices")
class JwtServiceTest {
    private static final String TEST_KEY = "swD1K5zvcITvpdpzWhJTfs68QrcLdoTpAbH9hjpxbFL";

    private JwtService jwtServiceWith(Duration duration, String key) {
        return new JwtService(new AppSecurityProperties(false, List.of(), key, duration, "unused", "unused"));
    }

    private final JwtService testService = jwtServiceWith(Duration.ofMinutes(5), TEST_KEY);

    @Test
    @DisplayName("should validate username when claim extraction is successful")
    void shouldValidateUsername_whenClaimExtraction_isSuccessful(){
        String expectedUsername = "testUsername";
        String token = testService.generateToken(expectedUsername);
        String actualUsername = testService.extractUsername(token);
        assertEquals(expectedUsername, actualUsername);
    }

    @Test
    @DisplayName("should throw exception when token is signed with another key")
    void shouldThrowException_whenTokenIsSignedWithAnotherKey(){
        JwtService jwtServiceWithAnotherKey = jwtServiceWith(Duration.ofMinutes(5), "VUC0ZdHDb3orC7IMws28oVGBrdcL4YlEWdbB13zdWYK");
        String token = testService.generateToken("testUsername");
        assertThrows(SignatureException.class, () -> jwtServiceWithAnotherKey.extractUsername(token));
    }

    @Test
    @DisplayName("should throw exception when token is expired")
    void shouldRejectToken_WhenTokenIsExpired(){
        JwtService expiredService = jwtServiceWith(Duration.ofSeconds(-1), TEST_KEY);
        String token = expiredService.generateToken("testUsername");
        assertThrows(ExpiredJwtException.class, () -> testService.extractUsername(token));
    }

    @Test
    @DisplayName("should throw exception when token is not signed")
    void shouldRejectToken_WhenTokenIsNotSigned(){
        String token = Jwts.builder().subject("unsigned").compact();
        assertThrows(UnsupportedJwtException.class, () -> testService.extractUsername(token));
    }
}