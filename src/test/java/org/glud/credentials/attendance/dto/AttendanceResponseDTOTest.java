package org.glud.credentials.attendance.dto;

import org.glud.credentials.attendance.model.Attendance;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttendanceResponseDTOTest {

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(1L);
        tenant.setName("GLUD");
        return tenant;
    }

    private User user(String codigo) {
        User user = new User();
        user.setUserId(2L);
        user.setCodigo(codigo);
        user.setName("Juan Pérez");
        user.setEmail("juan@glud.org");
        user.setRol(Rol.MIEMBRO);
        return user;
    }

    @Test
    void from_mapsAllFieldsIncludingMarkedBy() {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(10L);
        attendance.setTenant(tenant());
        attendance.setUser(user("20210000002"));
        attendance.setMarkedBy(user("20210000001"));
        attendance.setCheckInAt(LocalDateTime.of(2026, 8, 11, 8, 30));

        AttendanceResponseDTO dto = AttendanceResponseDTO.from(attendance);

        assertEquals(10L, dto.attendanceId());
        assertEquals("20210000002", dto.codigo());
        assertEquals("Juan Pérez", dto.name());
        assertEquals("juan@glud.org", dto.email());
        assertEquals(Rol.MIEMBRO, dto.rol());
        assertEquals(1L, dto.tenantId());
        assertEquals("GLUD", dto.tenantName());
        assertEquals(LocalDateTime.of(2026, 8, 11, 8, 30), dto.checkInAt());
        assertEquals("20210000001", dto.markedByCodigo());
    }

    @Test
    void from_handlesMissingMarkedByAndFallsBackToCodigoForName() {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(11L);
        attendance.setTenant(tenant());
        attendance.setUser(user("20210000002"));
        attendance.getUser().setName(null);
        attendance.setCheckInAt(LocalDateTime.of(2026, 8, 11, 8, 31));

        AttendanceResponseDTO dto = AttendanceResponseDTO.from(attendance);

        assertNull(dto.markedByCodigo());
        assertEquals("20210000002", dto.name());
    }
}
