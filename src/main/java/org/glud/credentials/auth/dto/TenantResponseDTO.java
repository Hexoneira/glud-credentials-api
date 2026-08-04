package org.glud.credentials.auth.dto;

import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.TenantStatus;

public record TenantResponseDTO(
        Long id,
        String name,
        String tenantCode,
        String director,
        Integer memberLimit,
        long currentMembers,
        TenantStatus status
) {
    public static TenantResponseDTO from(Tenant tenant, long currentMembers) {
        return new TenantResponseDTO(
                tenant.getTenantId(),
                tenant.getName(),
                tenant.getTenantCode(),
                tenant.getDirector(),
                tenant.getMemberLimit(),
                currentMembers,
                tenant.getStatus()
        );
    }
}
