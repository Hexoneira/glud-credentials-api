package org.glud.credentials;

import org.glud.credentials.auth.controller.GuestController;
import org.glud.credentials.auth.dto.CreateGuestRequestDTO;
import org.glud.credentials.auth.dto.GuestResponseDTO;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.service.GuestService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.exception.ActiveGuestAlreadyExistsException;
import org.glud.credentials.security.exception.GuestLimitExceededException;
import org.glud.credentials.security.exception.GuestLinkExpiredException;
import org.glud.credentials.security.exception.GuestNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuestController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestControllerTest {

    private static final GuestResponseDTO GUEST_DTO = new GuestResponseDTO(
            5L, "101011000", "Invitada Uno", "inv1@mail.com", GuestStatus.ACTIVE, 1L, "GLUD", "GLUD", "#22fefb", null,
            10L, "20210000001", null, null, "tok123", null);

    private static final String VALID_BODY = """
            {
              "codigo": "101011000",
              "name": "Invitada Uno",
              "email": "inv1@mail.com"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestService guestService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    @WithMockUser
    void create_returns201WithCreatedGuest() throws Exception {
        when(guestService.create(any(CreateGuestRequestDTO.class))).thenReturn(GUEST_DTO);

        mockMvc.perform(post("/api/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.codigo").value("101011000"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    void create_returns400_whenBodyIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "codigo": "ab",
                  "name": ""
                }
                """;

        mockMvc.perform(post("/api/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void create_returns409_whenCreatorHasActiveGuest() throws Exception {
        doThrow(new ActiveGuestAlreadyExistsException())
                .when(guestService).create(any(CreateGuestRequestDTO.class));

        mockMvc.perform(post("/api/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void create_returns403_whenTenantLimitExceeded() throws Exception {
        doThrow(new GuestLimitExceededException(15))
                .when(guestService).create(any(CreateGuestRequestDTO.class));

        mockMvc.perform(post("/api/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void create_returns403_whenNotAuthorized() throws Exception {
        doThrow(new RoleRequiredException(Rol.TENANT_ADMIN, Rol.MIEMBRO, Rol.SUPER_ADMIN))
                .when(guestService).create(any(CreateGuestRequestDTO.class));

        mockMvc.perform(post("/api/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getCurrent_returns200WithGuest() throws Exception {
        when(guestService.getCurrentGuest()).thenReturn(GUEST_DTO);

        mockMvc.perform(get("/api/guests/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    void getCurrent_returns404_whenNoActiveGuest() throws Exception {
        when(guestService.getCurrentGuest()).thenThrow(new GuestNotFoundException(10L));

        mockMvc.perform(get("/api/guests/current"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void listMine_returns200WithGuestList() throws Exception {
        when(guestService.listMyGuests()).thenReturn(List.of(GUEST_DTO));

        mockMvc.perform(get("/api/guests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void access_returns200WithGuest() throws Exception {
        when(guestService.accessGuest("tok123")).thenReturn(GUEST_DTO);

        mockMvc.perform(get("/api/guests/access/tok123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.codigo").value("101011000"));
    }

    @Test
    void access_returns410_whenLinkExpired() throws Exception {
        when(guestService.accessGuest("tok123")).thenThrow(new GuestLinkExpiredException());

        mockMvc.perform(get("/api/guests/access/tok123"))
                .andExpect(status().isGone());
    }

    @Test
    void access_returns404_whenUnknownToken() throws Exception {
        when(guestService.accessGuest("nope")).thenThrow(new GuestNotFoundException());

        mockMvc.perform(get("/api/guests/access/nope"))
                .andExpect(status().isNotFound());
    }
}
