package org.glud.credentials.auth.dto;

import org.glud.credentials.auth.model.User;

import java.util.List;

public record MemberCurrentResponseDTO(
        String id,
        String name,
        String email,
        String role,
        List<String> groups,
        String icon,
        String totpSecret
) {
    public static MemberCurrentResponseDTO from(User user) {
        return new MemberCurrentResponseDTO(
                user.getCodigo(),
                user.getCodigo(),
                user.getEmail(),
                user.getRol().name(),
                List.of(user.getTenant().getName()),
                null,
                null
        );
    }
}
