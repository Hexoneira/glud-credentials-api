package org.glud.credentials.auth.dto;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;

public record MemberResponseDTO(
        Long id,
        String codigo,
        String username,
        String email,
        Rol rol,
        UserStatus status,
        Long tenantId,
        String tenantName
) {
    public static MemberResponseDTO from(User user) {
        return new MemberResponseDTO(
                user.getUserId(),
                user.getCodigo(),
                user.getUsername(),
                user.getEmail(),
                user.getRol(),
                user.getStatus(),
                user.getTenant().getTenantId(),
                user.getTenant().getName()
        );
    }
}
