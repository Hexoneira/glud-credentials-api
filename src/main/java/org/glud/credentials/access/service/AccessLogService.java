package org.glud.credentials.access.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.access.model.AccessAudit;
import org.glud.credentials.access.model.AccessLog;
import org.glud.credentials.access.repository.AccessLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(AccessAudit audit) {
        AccessLog log = new AccessLog();
        log.setSubjectType(audit.subjectType());
        log.setSubjectId(audit.subjectId());
        log.setCodigo(audit.codigo());
        log.setTenant(audit.tenant());
        log.setTotpCode(audit.totpCode());
        log.setDeviceId(audit.deviceId());
        log.setLocation(audit.location());
        log.setResult(audit.result());
        accessLogRepository.save(log);
    }
}
