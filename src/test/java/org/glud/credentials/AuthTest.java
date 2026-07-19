package org.glud.credentials;

import org.glud.credentials.auth.controller.AuthController;
import org.glud.credentials.security.exception.InvalidCredentialsException;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.glud.credentials.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    void login_returnsToken_whenCredentialsAreValid() throws Exception {
        when(userService.login(any())).thenReturn("mockedJwtToken");

        String requestBody = """
                {
                  "username": "usuarioValido",
                  "password": "claveValida123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mockedJwtToken")));
    }

    @Test
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        doThrow(new InvalidCredentialsException("Usuario o contraseña incorrectos"))
                .when(userService).login(any());

        String requestBody = """
                {
                  "username": "usuarioValido",
                  "password": "claveIncorrecta"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns400_whenBodyIsInvalid() throws Exception {
        String invalidRequestBody = """
                {
                  "username": "",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }
}

