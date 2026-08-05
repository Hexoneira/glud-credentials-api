package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.NotNull;
import org.glud.credentials.auth.model.UserStatus;

public record UpdateMemberStatusRequestDTO(
        @NotNull
        UserStatus status
) {
}
