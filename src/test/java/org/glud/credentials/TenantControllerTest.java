package org.glud.credentials;

import org.glud.credentials.auth.controller.TenantController;
import org.glud.credentials.auth.dto.CreateTenantRequestDTO;
import org.glud.credentials.auth.dto.TenantResponseDTO;
import org.glud.credentials.auth.dto.UpdateTenantRequestDTO;
import org.glud.credentials.auth.model.TenantStatus;
import org.glud.credentials.auth.service.TenantService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.exception.SuperAdminRequiredException;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.glud.credentials.security.exception.TenantAlreadyExistsException;
import org.glud.credentials.security.exception.TenantHasMembersException;
import org.glud.credentials.security.exception.TenantNotFoundException;
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

@WebMvcTest(TenantController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantControllerTest {

    private static final TenantResponseDTO TENANT_DTO = new TenantResponseDTO(
            1L, "GLUD", "GLUD", "Directora GLUD", 100, 3L, TenantStatus.ACTIVE, "#22fefb", null);

    private static final String VALID_CREATE_BODY = """
            {
              "name": "Nuevo Grupo",
              "tenantCode": "NVO",
              "director": "Directora",
              "memberLimit": 50
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    @WithMockUser
    void findAll_returnsListOfTenants() throws Exception {
        when(tenantService.findAll()).thenReturn(List.of(TENANT_DTO));

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tenantCode").value("GLUD"))
                .andExpect(jsonPath("$[0].currentMembers").value(3));
    }

    @Test
    @WithMockUser
    void create_returns201WithCreatedTenant() throws Exception {
        when(tenantService.create(any(CreateTenantRequestDTO.class))).thenReturn(TENANT_DTO);

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void create_returns400_whenBodyIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "name": "ab",
                  "tenantCode": "a",
                  "director": "",
                  "memberLimit": 0
                }
                """;

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void create_returns400_whenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void create_returns409_whenTenantCodeAlreadyExists() throws Exception {
        doThrow(new TenantAlreadyExistsException("NVO"))
                .when(tenantService).create(any(CreateTenantRequestDTO.class));

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void update_returns200WithUpdatedTenant() throws Exception {
        when(tenantService.update(eq(1L), any(UpdateTenantRequestDTO.class))).thenReturn(TENANT_DTO);

        String body = """
                {
                  "name": "Actualizado",
                  "director": "Directora",
                  "memberLimit": 120
                }
                """;

        mockMvc.perform(put("/api/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("GLUD"));
    }

    @Test
    @WithMockUser
    void update_returns404_whenTenantNotFound() throws Exception {
        doThrow(new TenantNotFoundException(99L))
                .when(tenantService).update(eq(99L), any(UpdateTenantRequestDTO.class));

        String body = """
                {
                  "name": "Actualizado"
                }
                """;

        mockMvc.perform(put("/api/tenants/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void suspend_returns200WithSuspendedTenant() throws Exception {
        TenantResponseDTO suspended = new TenantResponseDTO(
                1L, "GLUD", "GLUD", "Directora GLUD", 100, 3L, TenantStatus.SUSPENDED, "#22fefb", null);
        when(tenantService.suspend(1L)).thenReturn(suspended);

        mockMvc.perform(patch("/api/tenants/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @WithMockUser
    void reactivate_returns200WithActiveTenant() throws Exception {
        when(tenantService.reactivate(1L)).thenReturn(TENANT_DTO);

        mockMvc.perform(patch("/api/tenants/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(tenantService).delete(1L);

        mockMvc.perform(delete("/api/tenants/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void delete_returns409_whenTenantHasMembers() throws Exception {
        doThrow(new TenantHasMembersException(1L)).when(tenantService).delete(1L);

        mockMvc.perform(delete("/api/tenants/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void anyEndpoint_returns403_whenNotSuperAdmin() throws Exception {
        doThrow(new SuperAdminRequiredException()).when(tenantService).findAll();

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isForbidden());
    }
}
