package org.glud.credentials.access.model;

import org.glud.credentials.auth.model.Tenant;

public record AccessAudit(
        SubjectType subjectType,
        Long subjectId,
        String codigo,
        Tenant tenant,
        String totpCode,
        String deviceId,
        String location,
        AccessResult result
) {
}
