package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.security.components.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private static final String TEST_SECRET = "k5oD8hP5mWg9p5NzT5y8aTU1DpxHiP7xWROpNuXIkub";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationTime", 86400000L);
    }

    @Test
    void generateJwtToken_createsValidToken() {
        String token = jwtUtils.generateJwtToken(1L, 10L, Rol.MIEMBRO);

        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("1", jwtUtils.getUserIdFromJwtToken(token));
    }

    @Test
    void validateJwtToken_returnsFalse_whenTokenIsMalformed() {
        assertFalse(jwtUtils.validateJwtToken("not.a.valid.token"));
    }

    @Test
    void getJwtFromHeader_returnsToken_whenBearerHeaderPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer myToken123");

        assertEquals("myToken123", jwtUtils.getJwtFromHeader(request));
    }

    @Test
    void getJwtFromHeader_returnsNull_whenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertNull(jwtUtils.getJwtFromHeader(request));
    }
}
