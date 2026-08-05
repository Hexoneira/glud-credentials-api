package org.glud.credentials.access.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.access.dto.AccessRequestDTO;
import org.glud.credentials.access.dto.AccessValidationResponseDTO;
import org.glud.credentials.access.event.AccessDecisionEvent;
import org.glud.credentials.access.model.AccessAudit;
import org.glud.credentials.access.model.AccessResult;
import org.glud.credentials.access.model.SubjectType;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.GuestRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.InvalidCredentialsException;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccessService {

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final TOTPService totpService;
    private final AccessLogService accessLogService;
    private final ApplicationEventPublisher eventPublisher;

    public AccessValidationResponseDTO validate(AccessRequestDTO request) throws NoSuchAlgorithmException, InvalidKeyException {
        Subject subject = resolveSubject(request);

        if (subject == null) {
            deny(request, SubjectType.UNKNOWN, null, null, "Credencial no válida");
        }
        if (!subject.active()) {
            deny(request, subject.type(), subject.id(), subject.tenant(), "Credencial no activa");
        }

        String seed = totpService.generateSeed(subject.codigo(), subject.tenantCode());
        if (!totpService.verify(seed, request.totpCode())) {
            deny(request, subject.type(), subject.id(), subject.tenant(), "Código TOTP inválido o expirado");
        }

        persist(request, subject, AccessResult.ALLOWED, "Acceso permitido");
        return new AccessValidationResponseDTO(true, "Acceso permitido", LocalDateTime.now());
    }

    private void deny(AccessRequestDTO request, SubjectType type, Long id, Tenant tenant, String message) {
        persist(request, type, id, tenant, AccessResult.DENIED, message);
        throw new InvalidCredentialsException(message);
    }

    private void persist(AccessRequestDTO request, Subject subject, AccessResult result, String message) {
        persist(request, subject.type(), subject.id(), subject.tenant(), result, message);
    }

    private void persist(AccessRequestDTO request, SubjectType type, Long id, Tenant tenant,
                         AccessResult result, String message) {
        accessLogService.persist(new AccessAudit(
                type, id, request.codigo(), tenant, request.totpCode(), request.deviceId(), request.location(), result));
        eventPublisher.publishEvent(new AccessDecisionEvent(
                id, type, tenant != null ? tenant.getTenantId() : null, request.codigo(), result, message, LocalDateTime.now()));
    }

    private Subject resolveSubject(AccessRequestDTO request) {
        return switch (request.subjectType()) {
            case GUEST -> guestRepository
                    .findByCodigoAndTenantTenantId(request.codigo(), currentTenantId())
                    .map(guest -> new Subject(SubjectType.GUEST, guest.getGuestId(), guest.getCodigo(),
                            guest.getTenant().getTenantCode(), guest.getTenant(),
                            guest.getStatus() == GuestStatus.ACTIVE))
                    .orElse(null);
            case MEMBER -> userRepository
                    .findByCodigo(request.codigo())
                    .map(user -> new Subject(SubjectType.MEMBER, user.getUserId(), user.getCodigo(),
                            user.getTenant().getTenantCode(), user.getTenant(),
                            user.getStatus() == UserStatus.ACTIVE))
                    .orElse(null);
            case UNKNOWN -> null;
        };
    }

    private Long currentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl principal) {
            return principal.getTenantId();
        }
        return null;
    }

    private record Subject(SubjectType type, Long id, String codigo, String tenantCode, Tenant tenant, boolean active) {
    }
}
