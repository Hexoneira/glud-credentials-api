package org.glud.credentials.security.components;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JwtUtilsSecretValidationTest {

    private final JwtUtils jwtUtils = new JwtUtils();
    private static final String VALID_SECRET = "k5oD8hP5mWg9p5NzT5y8aTU1DpxHiP7xWROpNuXIkub";

    @Test
    void validateSecret_acceptsValidBase64Secret() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", VALID_SECRET);

        assertDoesNotThrow(() -> jwtUtils.validateSecret());
    }

    @Test
    void validateSecret_throwsWhenSecretIsMissing() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", null);

        assertThrows(IllegalStateException.class, () -> jwtUtils.validateSecret());
    }

    @Test
    void validateSecret_throwsWhenSecretIsBlank() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "   ");

        assertThrows(IllegalStateException.class, () -> jwtUtils.validateSecret());
    }

    @Test
    void validateSecret_throwsWhenSecretIsInvalidBase64() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "not-valid-base64!!");

        assertThrows(IllegalStateException.class, () -> jwtUtils.validateSecret());
    }
}
