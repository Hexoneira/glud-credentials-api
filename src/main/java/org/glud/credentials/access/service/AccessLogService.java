package org.glud.credentials.access.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.access.model.AccessLog;
import org.glud.credentials.access.model.AccessResult;
import org.glud.credentials.access.model.SubjectType;
import org.glud.credentials.access.repository.AccessLogRepository;
import org.glud.credentials.auth.model.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SubjectType subjectType, Long subjectId, Tenant tenant,
                       String totpCode, String deviceId, String location, AccessResult result) {
        AccessLog log = new AccessLog();
        log.setSubjectType(subjectType);
        log.setSubjectId(subjectId);
        log.setTenant(tenant);
        log.setTotpCode(totpCode);
        log.setDeviceId(deviceId);
        log.setLocation(location);
        log.setResult(result);
        accessLogRepository.save(log);
    }
}
