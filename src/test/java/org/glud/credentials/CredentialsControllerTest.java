package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.service.MemberService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.glud.credentials.totp_seed.controller.CredentialsController;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CredentialsController.class)
@AutoConfigureMockMvc(addFilters = false)
class CredentialsControllerTest {

    private static final String SEED = "4ljjwnrzlorsnlhdmitrl4rubdftyhc64bt3qqsnhjbdbq2uqyhq";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TOTPService totpService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User member() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(1L);
        tenant.setTenantCode("GLUD");
        User user = new User();
        user.setUserId(5L);
        user.setCodigo("20210000000");
        user.setTenant(tenant);
        user.setRol(Rol.MIEMBRO);
        return user;
    }

    @Test
    void getSeed_returns200WithBase32Seed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserDetailsImpl(5L, "20210000000", "pw", 1L, Rol.MIEMBRO, Collections.emptyList()),
                        null,
                        Collections.emptyList()
                )
        );
        when(memberService.loadUser(5L)).thenReturn(member());
        when(totpService.generateSeed("20210000000", "GLUD")).thenReturn(SEED);

        mockMvc.perform(get("/api/credentials/seed"))
                .andExpect(status().isOk())
                .andExpect(content().string(SEED));
    }

    @Test
    void getSeed_returns404_whenMemberNotFound() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserDetailsImpl(99L, "20210000000", "pw", 1L, Rol.MIEMBRO, Collections.emptyList()),
                        null,
                        Collections.emptyList()
                )
        );
        when(memberService.loadUser(99L)).thenThrow(new MemberNotFoundException(99L));

        mockMvc.perform(get("/api/credentials/seed"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSeed_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/credentials/seed"))
                .andExpect(status().isUnauthorized());
    }
}