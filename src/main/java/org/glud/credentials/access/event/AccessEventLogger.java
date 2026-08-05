package org.glud.credentials.access.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccessEventLogger {

    @EventListener(AccessDecisionEvent.class)
    public void onAccessDecision(AccessDecisionEvent event) {
        log.info("Acceso {} para subjectId={} ({}) codigo={} en tenant={}: {}",
                event.result(), event.subjectId(), event.subjectType(), event.codigo(), event.tenantId(), event.message());
    }
}
