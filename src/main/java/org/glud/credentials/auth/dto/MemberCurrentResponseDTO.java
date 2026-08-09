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
        String totpSecret,
        String tenantName,
        String tenantCode,
        String primaryColor,
        String logoUrl
) {
    public static MemberCurrentResponseDTO from(User user) {
        return from(user, null);
    }

    public static MemberCurrentResponseDTO from(User user, String totpSecret) {
        String name = user.getName();
        return new MemberCurrentResponseDTO(
                user.getCodigo(),
                name != null ? name : user.getCodigo(),
                user.getEmail(),
                user.getRol().name(),
                List.of(user.getTenant().getName()),
                null,
                totpSecret,
                user.getTenant().getName(),
                user.getTenant().getTenantCode(),
                user.getTenant().getPrimaryColor(),
                user.getTenant().getLogoUrl()
        );
    }
}