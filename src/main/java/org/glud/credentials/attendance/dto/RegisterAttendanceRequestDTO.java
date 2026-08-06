package org.glud.credentials.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterAttendanceRequestDTO(
        @NotBlank String code
) {
}
