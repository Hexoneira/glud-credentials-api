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
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleGuard roleGuard;

    @Value("${app.guest.max-active}")
    private int maxActiveGuests;

    @Transactional
    public GuestResponseDTO create(CreateGuestRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.MIEMBRO, Rol.SUPER_ADMIN);
        Long tenantId = currentTenantId();
        Long userId = currentUserId();

        Tenant tenant = tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (guestRepository.existsByCreatedByUserIdAndStatus(userId, GuestStatus.ACTIVE)) {
            throw new ActiveGuestAlreadyExistsException();
        }
        if (guestRepository.countByTenantTenantIdAndStatus(tenantId, GuestStatus.ACTIVE) >= maxActiveGuests) {
            throw new GuestLimitExceededException(maxActiveGuests);
        }

        Guest guest = new Guest();
        guest.setCodigo(request.codigo());
        guest.setName(request.name());
        guest.setEmail(request.email());
        guest.setTenant(tenant);
        guest.setCreatedBy(userRepository.getReferenceById(userId));
        guest.setStatus(GuestStatus.ACTIVE);

        return GuestResponseDTO.from(guestRepository.save(guest));
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
