package org.glud.credentials;

import org.glud.credentials.security.config.AuthEntryPointJwt;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AuthEntryPointJwtTest {

    @Test
    void commence_setsUnauthorizedResponse() throws IOException {
        AuthEntryPointJwt entryPoint = new AuthEntryPointJwt();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Usuario o contraseña incorrecto"));
        assertTrue(response.getContentAsString().contains("/api/protected"));
    }
}
