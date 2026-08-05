package org.glud.credentials;

import org.glud.credentials.access.controller.AccessController;
import org.glud.credentials.access.dto.AccessRequestDTO;
import org.glud.credentials.access.dto.AccessValidationResponseDTO;
import org.glud.credentials.access.service.AccessService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.exception.InvalidCredentialsException;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccessController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccessControllerTest {

    private static final String VALID_BODY = """
            {
              "userId": 1,
              "totpCode": "123456",
              "deviceId": "escanner-01",
              "location": "Puerta principal"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessService accessService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    @WithMockUser
    void validate_returns200WhenAllowed() throws Exception {
        when(accessService.validate(any(AccessRequestDTO.class)))
                .thenReturn(new AccessValidationResponseDTO(true, "Acceso permitido", LocalDateTime.now()));

        mockMvc.perform(post("/api/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.message").value("Acceso permitido"));
    }

    @Test
    @WithMockUser
    void validate_returns401WhenTotpInvalid() throws Exception {
        doThrow(new InvalidCredentialsException("Código TOTP inválido o expirado"))
                .when(accessService).validate(any(AccessRequestDTO.class));

        mockMvc.perform(post("/api/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Código TOTP inválido o expirado"));
    }

    @Test
    @WithMockUser
    void validate_returns400_whenBodyIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "totpCode": "12"
                }
                """;

        mockMvc.perform(post("/api/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void validate_returns400_whenTotpCodeNotNumeric() throws Exception {
        String invalidBody = """
                {
                  "userId": 1,
                  "totpCode": "abcdef"
                }
                """;

        mockMvc.perform(post("/api/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
