package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.Email;
import org.glud.credentials.auth.model.Rol;

public record UpdateMemberRequestDTO(
        @Email
        String email,
        Rol rol
) {
}
