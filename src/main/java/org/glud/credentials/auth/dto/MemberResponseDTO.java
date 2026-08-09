package org.glud.credentials.auth.dto;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;

public record MemberResponseDTO(
        Long id,
        String codigo,
        String username,
        String name,
        Rol rol,
        UserStatus status,
        Long tenantId,
        String tenantName
) {
    public static MemberResponseDTO from(User user) {
        String name = user.getName();
        return new MemberResponseDTO(
                user.getUserId(),
                user.getCodigo(),
                user.getUsername(),
                name != null ? name : user.getUsername(),
                user.getRol(),
                user.getStatus(),
                user.getTenant().getTenantId(),
                user.getTenant().getName()
        );
    }
}
