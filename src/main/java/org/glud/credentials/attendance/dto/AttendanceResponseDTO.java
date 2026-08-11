package org.glud.credentials.attendance.dto;

import org.glud.credentials.attendance.model.Attendance;
import org.glud.credentials.auth.model.Rol;

import java.time.LocalDateTime;

public record AttendanceResponseDTO(
        Long attendanceId,
        String codigo,
        String name,
        String email,
        Rol rol,
        Long tenantId,
        String tenantName,
        LocalDateTime checkInAt,
        String markedByCodigo
) {
    public static AttendanceResponseDTO from(Attendance attendance) {
        String name = attendance.getUser().getName();
        return new AttendanceResponseDTO(
                attendance.getAttendanceId(),
                attendance.getUser().getCodigo(),
                name != null ? name : attendance.getUser().getCodigo(),
                attendance.getUser().getEmail(),
                attendance.getUser().getRol(),
                attendance.getTenant().getTenantId(),
                attendance.getTenant().getName(),
                attendance.getCheckInAt(),
                attendance.getMarkedBy() != null ? attendance.getMarkedBy().getCodigo() : null
        );
    }
}
