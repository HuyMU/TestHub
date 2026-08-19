package com.testhub.testflowlite.security;

import com.testhub.testflowlite.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderUnitTest {

    private static final String KNOWN_LEAKED_DEFAULT =
            "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b";

    private static final String CUSTOM_SECRET =
            "11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff";

    private JwtConfig jwtConfig;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret(CUSTOM_SECRET);
        jwtConfig.setExpirationMs(3600000L);
        jwtConfig.setRefreshExpirationMs(86400000L);

        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    @Test
    void testWarnIfUsingLeakedDefaultSecret_WithLeakedValue_DoesNotThrow() {
        jwtConfig.setSecret(KNOWN_LEAKED_DEFAULT);

        assertDoesNotThrow(() -> jwtTokenProvider.warnIfUsingLeakedDefaultSecret());
    }

    @Test
    void testWarnIfUsingLeakedDefaultSecret_WithCustomSecret_DoesNotThrow() {
        jwtConfig.setSecret(CUSTOM_SECRET);

        assertDoesNotThrow(() -> jwtTokenProvider.warnIfUsingLeakedDefaultSecret());
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken("testuser", "LEADER");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateAccessToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    void testGenerateAndValidateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken("testuser");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateRefreshToken(token));
        assertFalse(jwtTokenProvider.validateAccessToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }
}
