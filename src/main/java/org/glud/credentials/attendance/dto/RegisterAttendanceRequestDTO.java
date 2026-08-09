package org.glud.credentials.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterAttendanceRequestDTO(
        @NotBlank String code,
        // Si llega, la asistencia se registra ligada a ese evento (con dedup por evento)
        Long eventId
) {
}
