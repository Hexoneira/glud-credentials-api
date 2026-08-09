package org.glud.credentials;

import org.glud.credentials.attendance.controller.AttendanceController;
import org.glud.credentials.attendance.dto.AttendanceResponseDTO;
import org.glud.credentials.attendance.service.AttendanceService;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.security.exception.AttendanceAlreadyExistsException;
import org.glud.credentials.security.exception.InvalidScannedCodeException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.middleware.RequiredAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class AttendanceControllerTest {

    private static final AttendanceResponseDTO ATTENDANCE_DTO = new AttendanceResponseDTO(
            1L, "20210000002", "María Gómez", "m2@glud.org", Rol.MIEMBRO, 1L, "GLUD",
            LocalDateTime.of(2026, 8, 5, 9, 30), "20219999999");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private RequiredAuth requiredAuth;

    @Test
    void register_returns201WithAttendance() throws Exception {
        when(attendanceService.registerAttendance(any())).thenReturn(ATTENDANCE_DTO);

        mockMvc.perform(post("/api/attendance/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "20210000002" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("20210000002"))
                .andExpect(jsonPath("$.tenantName").value("GLUD"))
                .andExpect(jsonPath("$.markedByCodigo").value("20219999999"));
    }

    @Test
    void register_returns400_whenScannedCodeInvalid() throws Exception {
        doThrow(new InvalidScannedCodeException())
                .when(attendanceService).registerAttendance(any());

        mockMvc.perform(post("/api/attendance/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "no válido" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns409_whenAlreadyRegisteredToday() throws Exception {
        doThrow(new AttendanceAlreadyExistsException("20210000002"))
                .when(attendanceService).registerAttendance(any());

        mockMvc.perform(post("/api/attendance/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "20210000002" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns403_whenNotAdmin() throws Exception {
        doThrow(new RoleRequiredException(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN))
                .when(attendanceService).registerAttendance(any());

        mockMvc.perform(post("/api/attendance/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "20210000002" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void today_returnsList() throws Exception {
        when(attendanceService.todayAttendance()).thenReturn(List.of(ATTENDANCE_DTO));

        mockMvc.perform(get("/api/attendance/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("20210000002"));
    }

    @Test
    void todayExport_returnsCsvAttachment() throws Exception {
        when(attendanceService.exportTodayCsv()).thenReturn("""
                \uFEFFCódigo;Nombre;Rol;Grupo;Hora;Registrado por
                "20210000002";"María Gómez";"MIEMBRO";"GLUD";"09:30:00";"20219999999"
                """);

        mockMvc.perform(get("/api/attendance/today/export"))
                .andExpect(status().isOk())
                .andExpect(result -> result.getResponse().getContentType().startsWith("text/csv"))
                .andExpect(result -> result.getResponse().getHeader("Content-Disposition")
                        .contains("attachment; filename=\"asistencia-"))
                .andExpect(result -> result.getResponse().getContentAsString()
                        .contains("María Gómez"));
    }

    @Test
    void register_acceptsEventIdAndTotpPayload() throws Exception {
        when(attendanceService.registerAttendance(any())).thenReturn(ATTENDANCE_DTO);

        mockMvc.perform(post("/api/attendance/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "ID:20210000002|TOTP:123456", "eventId": 50 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("20210000002"));
    }
}
