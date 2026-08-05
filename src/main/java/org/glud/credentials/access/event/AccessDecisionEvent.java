package org.glud.credentials.access.event;

import org.glud.credentials.access.model.AccessResult;
import org.glud.credentials.access.model.SubjectType;

import java.time.LocalDateTime;

public record AccessDecisionEvent(
        Long subjectId,
        SubjectType subjectType,
        Long tenantId,
        AccessResult result,
        String message,
        LocalDateTime timestamp
) {
}
