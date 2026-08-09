package org.glud.credentials.event.dto;

import org.glud.credentials.attendance.model.Attendance;

import java.time.LocalDateTime;

public record EventAttendanceResponseDTO(
        Long attendanceId,
        Long userId,
        String codigo,
        String name,
        LocalDateTime checkInAt,
        String markedByCodigo
) {
    public static EventAttendanceResponseDTO from(Attendance attendance) {
        String name = attendance.getUser().getName();
        return new EventAttendanceResponseDTO(
                attendance.getAttendanceId(),
                attendance.getUser().getUserId(),
                attendance.getUser().getCodigo(),
                name != null ? name : attendance.getUser().getCodigo(),
                attendance.getCheckInAt(),
                attendance.getMarkedBy().getCodigo()
        );
    }
}
