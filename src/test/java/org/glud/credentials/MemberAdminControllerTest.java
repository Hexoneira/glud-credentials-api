package org.glud.credentials;

import org.glud.credentials.auth.controller.MemberAdminController;
import org.glud.credentials.auth.dto.CreateMemberRequestDTO;
import org.glud.credentials.auth.dto.MemberResponseDTO;
import org.glud.credentials.auth.dto.UpdateMemberRequestDTO;
import org.glud.credentials.auth.dto.UpdateMemberStatusRequestDTO;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.service.MemberAdminService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
import org.glud.credentials.security.exception.MemberAlreadyExistsException;
import org.glud.credentials.security.exception.MemberNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberAdminControllerTest {

    private static final MemberResponseDTO MEMBER_DTO = new MemberResponseDTO(
            2L, "20210000002", "20210000002", "m2@glud.org", Rol.MIEMBRO, UserStatus.ACTIVE, 1L, "GLUD");

    private static final String VALID_CREATE_BODY = """
            {
              "codigo": "20210000002",
              "password": "clave123",
              "email": "m2@glud.org",
              "rol": "MIEMBRO"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberAdminService memberAdminService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    @WithMockUser
    void findAll_returnsListOfMembers() throws Exception {
        when(memberAdminService.findAll()).thenReturn(List.of(MEMBER_DTO));

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("20210000002"))
                .andExpect(jsonPath("$[0].tenantId").value(1));
    }

    @Test
    @WithMockUser
    void create_returns201WithCreatedMember() throws Exception {
        when(memberAdminService.create(any(CreateMemberRequestDTO.class))).thenReturn(MEMBER_DTO);

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @WithMockUser
    void create_returns400_whenBodyIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "codigo": "ab",
                  "password": "123",
                  "rol": "MIEMBRO"
                }
                """;

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void create_returns409_whenCodigoAlreadyExists() throws Exception {
        doThrow(new MemberAlreadyExistsException("20210000002"))
                .when(memberAdminService).create(any(CreateMemberRequestDTO.class));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void update_returns200WithUpdatedMember() throws Exception {
        when(memberAdminService.update(eq(2L), any(UpdateMemberRequestDTO.class))).thenReturn(MEMBER_DTO);

        String body = """
                {
                  "email": "nuevo@glud.org",
                  "rol": "INVITADO"
                }
                """;

        mockMvc.perform(put("/api/members/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("MIEMBRO"));
    }

    @Test
    @WithMockUser
    void update_returns403_whenMemberBelongsToOtherTenant() throws Exception {
        doThrow(new CrossTenantAccessException())
                .when(memberAdminService).update(eq(2L), any(UpdateMemberRequestDTO.class));

        String body = """
                {
                  "email": "nuevo@glud.org"
                }
                """;

        mockMvc.perform(put("/api/members/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void update_returns404_whenMemberNotFound() throws Exception {
        doThrow(new MemberNotFoundException(99L))
                .when(memberAdminService).update(eq(99L), any(UpdateMemberRequestDTO.class));

        String body = """
                {
                  "email": "nuevo@glud.org"
                }
                """;

        mockMvc.perform(put("/api/members/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateStatus_returns200WithSuspendedMember() throws Exception {
        MemberResponseDTO suspended = new MemberResponseDTO(
                2L, "20210000002", "20210000002", "m2@glud.org", Rol.MIEMBRO, UserStatus.SUSPENDED, 1L, "GLUD");
        when(memberAdminService.updateStatus(eq(2L), any(UpdateMemberStatusRequestDTO.class))).thenReturn(suspended);

        String body = """
                {
                  "status": "SUSPENDED"
                }
                """;

        mockMvc.perform(patch("/api/members/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @WithMockUser
    void updateStatus_returns409_whenModifyingOwnAccount() throws Exception {
        doThrow(new InvalidMemberActionException("No puede modificar su propia cuenta"))
                .when(memberAdminService).updateStatus(eq(2L), any(UpdateMemberStatusRequestDTO.class));

        String body = """
                {
                  "status": "SUSPENDED"
                }
                """;

        mockMvc.perform(patch("/api/members/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(memberAdminService).delete(2L);

        mockMvc.perform(delete("/api/members/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void delete_returns403_whenMemberBelongsToOtherTenant() throws Exception {
        doThrow(new CrossTenantAccessException()).when(memberAdminService).delete(2L);

        mockMvc.perform(delete("/api/members/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void anyEndpoint_returns403_whenNotAdmin() throws Exception {
        doThrow(new RoleRequiredException(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN))
                .when(memberAdminService).findAll();

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isForbidden());
    }
}
