package org.glud.credentials;

import org.glud.credentials.auth.controller.MemberController;
import org.glud.credentials.auth.dto.MemberCurrentResponseDTO;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.service.MemberService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.middleware.RequiredAuth;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    private static final MemberCurrentResponseDTO MEMBER_DTO = new MemberCurrentResponseDTO(
            "20210000000", "20210000000", "miembro@udistrital.edu.co", "MIEMBRO",
            List.of("GLUD"), null, null, "GLUD", "GLUD", "#22fefb", null);

    @Autowired
    private MockMvc mockMvc;

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

    private void setPrincipal(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserDetailsImpl(userId, "20210000000", "pw", 1L, Rol.MIEMBRO, Collections.emptyList()),
                        null,
                        Collections.emptyList()
                )
        );
    }

    @Test
    void getCurrent_returns200WithMemberCredential() throws Exception {
        setPrincipal(1L);
        when(memberService.getCurrentMember(1L)).thenReturn(MEMBER_DTO);

        mockMvc.perform(get("/api/member/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("20210000000"))
                .andExpect(jsonPath("$.name").value("20210000000"))
                .andExpect(jsonPath("$.email").value("miembro@udistrital.edu.co"))
                .andExpect(jsonPath("$.role").value("MIEMBRO"))
                .andExpect(jsonPath("$.groups[0]").value("GLUD"));
    }

    @Test
    void getCurrent_returns404_whenMemberNotFound() throws Exception {
        setPrincipal(99L);
        when(memberService.getCurrentMember(99L)).thenThrow(new MemberNotFoundException(99L));

        mockMvc.perform(get("/api/member/current"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Miembro no encontrado: 99"));
    }

    @Test
    void getCurrent_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/member/current"))
                .andExpect(status().isUnauthorized());
    }
}
