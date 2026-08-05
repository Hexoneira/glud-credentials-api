package org.glud.credentials.auth.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.CreateTenantRequestDTO;
import org.glud.credentials.auth.dto.TenantResponseDTO;
import org.glud.credentials.auth.dto.UpdateTenantRequestDTO;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.TenantStatus;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.authorization.SuperAdminGuard;
import org.glud.credentials.security.exception.TenantAlreadyExistsException;
import org.glud.credentials.security.exception.TenantHasMembersException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SuperAdminGuard superAdminGuard;

    @Transactional(readOnly = true)
    public List<TenantResponseDTO> findAll() {
        superAdminGuard.assertSuperAdmin();
        return tenantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TenantResponseDTO create(CreateTenantRequestDTO request) {
        superAdminGuard.assertSuperAdmin();
        if (tenantRepository.existsByTenantCode(request.tenantCode())) {
            throw new TenantAlreadyExistsException(request.tenantCode());
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setTenantCode(request.tenantCode());
        tenant.setDirector(request.director());
        tenant.setMemberLimit(request.memberLimit());
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setPrimaryColor(normalizeColor(request.primaryColor()));
        tenant.setLogoUrl(request.logoUrl());

        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponseDTO update(Long id, UpdateTenantRequestDTO request) {
        superAdminGuard.assertSuperAdmin();
        Tenant tenant = findOrThrow(id);

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.director() != null) {
            tenant.setDirector(request.director());
        }
        if (request.memberLimit() != null) {
            tenant.setMemberLimit(request.memberLimit());
        }
        if (request.primaryColor() != null) {
            tenant.setPrimaryColor(normalizeColor(request.primaryColor()));
        }
        if (request.logoUrl() != null) {
            tenant.setLogoUrl(request.logoUrl());
        }

        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponseDTO suspend(Long id) {
        superAdminGuard.assertSuperAdmin();
        Tenant tenant = findOrThrow(id);
        if (tenant.getStatus() == TenantStatus.ACTIVE) {
            tenant.setStatus(TenantStatus.SUSPENDED);
            return toResponse(tenantRepository.save(tenant));
        }
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponseDTO reactivate(Long id) {
        superAdminGuard.assertSuperAdmin();
        Tenant tenant = findOrThrow(id);
        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            tenant.setStatus(TenantStatus.ACTIVE);
            return toResponse(tenantRepository.save(tenant));
        }
        return toResponse(tenant);
    }

    @Transactional
    public void delete(Long id) {
        superAdminGuard.assertSuperAdmin();
        Tenant tenant = findOrThrow(id);
        if (userRepository.countByTenantTenantId(id) > 0) {
            throw new TenantHasMembersException(id);
        }
        tenantRepository.delete(tenant);
    }

    private Tenant findOrThrow(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return "#22fefb";
        }
        return color.startsWith("#") ? color : "#" + color;
    }

    private TenantResponseDTO toResponse(Tenant tenant) {
        long currentMembers = userRepository.countByTenantTenantId(tenant.getTenantId());
        return TenantResponseDTO.from(tenant, currentMembers);
    }
}
