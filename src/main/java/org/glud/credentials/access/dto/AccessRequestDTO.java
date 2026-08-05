package org.glud.credentials.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.glud.credentials.access.model.SubjectType;

public record AccessRequestDTO(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 255)
        String codigo,
        SubjectType subjectType,
        @NotBlank(message = "El código TOTP es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El código TOTP debe tener 6 dígitos")
        String totpCode,
        String deviceId,
        String location
) {
    public SubjectType subjectType() {
        return subjectType == null ? SubjectType.MEMBER : subjectType;
    }
}
