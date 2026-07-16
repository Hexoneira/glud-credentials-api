package org.glud.credentials.auth.utilities;

import jakarta.validation.constraints.NotBlank;

public record RequestDTO(
        @NotBlank
        String username,
        @NotBlank
        String password
) {
}
