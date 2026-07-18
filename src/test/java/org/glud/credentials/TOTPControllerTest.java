package org.glud.credentials;

import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.glud.credentials.totp_seed.controller.TOTPController;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(TOTPController.class)
class TotpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TOTPService totpService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    @WithMockUser
    void generateSeed_returnsSeedFromService() throws Exception {
        when(totpService.generateSeed("12345")).thenReturn("mockedSeed1234");

        mockMvc.perform(post("/api/totp/generate-seed/12345").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("mockedSeed1234"));
    }
}
