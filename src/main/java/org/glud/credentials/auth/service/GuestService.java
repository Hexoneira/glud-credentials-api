package org.glud.credentials.auth.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.CreateGuestRequestDTO;
import org.glud.credentials.auth.dto.GuestResponseDTO;
import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.repository.GuestRepository;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.ActiveGuestAlreadyExistsException;
import org.glud.credentials.security.exception.GuestLimitExceededException;
import org.glud.credentials.security.exception.GuestLinkExpiredException;
import org.glud.credentials.security.exception.GuestNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleGuard roleGuard;
    private final TOTPService totpService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.guest.max-active}")
    private int maxActiveGuests;

    @Value("${app.guest.access-duration}")
    private Duration accessDuration;

    @Value("${app.guest.retention}")
    private Duration retention;

    @Transactional
    public GuestResponseDTO create(CreateGuestRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.MIEMBRO, Rol.SUPER_ADMIN);
        Long tenantId = currentTenantId();
        Long userId = currentUserId();

        Tenant tenant = tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        expireStaleGuests(userId);

        if (guestRepository.existsByCreatedByUserIdAndStatus(userId, GuestStatus.ACTIVE)) {
            throw new ActiveGuestAlreadyExistsException();
        }
        if (guestRepository.countByTenantTenantIdAndStatus(tenantId, GuestStatus.ACTIVE) >= maxActiveGuests) {
            throw new GuestLimitExceededException(maxActiveGuests);
        }

        LocalDateTime now = LocalDateTime.now();
        Guest guest = new Guest();
        guest.setCodigo(request.codigo());
        guest.setName(request.name());
        guest.setEmail(request.email());
        guest.setTenant(tenant);
        guest.setCreatedBy(userRepository.getReferenceById(userId));
        guest.setStatus(GuestStatus.ACTIVE);
        guest.setAccessToken(generateAccessToken());
        guest.setExpiresAt(now.plus(accessDuration));

        return GuestResponseDTO.from(guestRepository.save(guest), generateSeed(guest));
    }

    @Transactional(readOnly = true)
    public GuestResponseDTO getCurrentGuest() {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.MIEMBRO, Rol.SUPER_ADMIN);
        Long userId = currentUserId();

        Guest guest = guestRepository.findByCreatedByUserIdAndStatus(userId, GuestStatus.ACTIVE)
                .orElseThrow(() -> new GuestNotFoundException(userId));
        return GuestResponseDTO.from(guest, generateSeed(guest));
    }

    @Transactional(readOnly = true)
    public List<GuestResponseDTO> listMyGuests() {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.MIEMBRO, Rol.SUPER_ADMIN);
        Long userId = currentUserId();
        return guestRepository.findAllByCreatedByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(guest -> GuestResponseDTO.from(guest, guest.getStatus() == GuestStatus.ACTIVE ? generateSeed(guest) : null))
                .toList();
    }

    @Transactional
    public GuestResponseDTO accessGuest(String accessToken) {
        Guest guest = guestRepository.findByAccessToken(accessToken)
                .orElseThrow(GuestNotFoundException::new);

        if (guest.getStatus() != GuestStatus.ACTIVE || isExpired(guest)) {
            if (guest.getStatus() == GuestStatus.ACTIVE) {
                guest.setStatus(GuestStatus.EXPIRED);
                guestRepository.save(guest);
            }
            throw new GuestLinkExpiredException();
        }
        return GuestResponseDTO.from(guest, generateSeed(guest));
    }

    @Scheduled(cron = "${app.guest.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredGuests() {
        LocalDateTime cutoff = LocalDateTime.now().minus(retention);
        guestRepository.deleteByCreatedAtBefore(cutoff);
    }

    private void expireStaleGuests(Long userId) {
        guestRepository.findByCreatedByUserIdAndStatus(userId, GuestStatus.ACTIVE)
                .filter(this::isExpired)
                .ifPresent(guest -> {
                    guest.setStatus(GuestStatus.EXPIRED);
                    guestRepository.save(guest);
                });
    }

    private boolean isExpired(Guest guest) {
        return guest.getExpiresAt() != null && guest.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private String generateAccessToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String generateSeed(Guest guest) {
        try {
            return totpService.generateSeed(guest.getCodigo(), guest.getTenant().getTenantCode());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private Long currentTenantId() {
        return principal().getTenantId();
    }

    private Long currentUserId() {
        return principal().getUserId();
    }

    private UserDetailsImpl principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new RoleRequiredException(Rol.TENANT_ADMIN, Rol.MIEMBRO, Rol.SUPER_ADMIN);
        }
        return principal;
    }
}
