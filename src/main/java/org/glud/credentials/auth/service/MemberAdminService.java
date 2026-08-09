package org.glud.credentials.auth.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.CreateMemberRequestDTO;
import org.glud.credentials.auth.dto.MemberResponseDTO;
import org.glud.credentials.auth.dto.UpdateMemberRequestDTO;
import org.glud.credentials.auth.dto.UpdateMemberStatusRequestDTO;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
import org.glud.credentials.security.exception.MemberAlreadyExistsException;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberAdminService {

    public static final int MAX_ADMINS_PER_TENANT = 2;

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleGuard roleGuard;

    @Transactional(readOnly = true)
    public List<MemberResponseDTO> findAll(Long tenantId) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        if (currentIsSuperAdmin()) {
            if (tenantId != null) {
                return userRepository.findByTenantTenantId(tenantId).stream()
                        .map(MemberResponseDTO::from)
                        .toList();
            }
            return userRepository.findAll().stream()
                    .map(MemberResponseDTO::from)
                    .toList();
        }
        return userRepository.findByTenantTenantId(currentTenantId()).stream()
                .map(MemberResponseDTO::from)
                .toList();
    }

    @Transactional
    public MemberResponseDTO create(CreateMemberRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);

        if (request.rol() == Rol.SUPER_ADMIN && !currentIsSuperAdmin()) {
            throw new InvalidMemberActionException("Solo el super admin puede otorgar el rol SUPER_ADMIN");
        }
        if (userRepository.existsByCodigo(request.codigo())) {
            throw new MemberAlreadyExistsException("Ya existe un miembro con el código " + request.codigo());
        }

        Tenant tenant = resolveTenant(request.tenantId());
        assertAdminLimit(tenant.getTenantId(), request.rol());

        User user = new User();
        user.setCodigo(request.codigo());
        user.setUsername(request.codigo());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name().trim());
        user.setRol(request.rol());
        user.setStatus(UserStatus.ACTIVE);
        user.setTenant(tenant);

        return MemberResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    public MemberResponseDTO update(Long id, UpdateMemberRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        User user = findById(id);

        if (request.rol() != null) {
            if (id.equals(currentUserId())) {
                throw new InvalidMemberActionException("No puede cambiar su propio rol");
            }
            if (request.rol() == Rol.SUPER_ADMIN && !currentIsSuperAdmin()) {
                throw new InvalidMemberActionException("Solo el super admin puede otorgar el rol SUPER_ADMIN");
            }
            if (!currentIsSuperAdmin() && user.getRol() == Rol.SUPER_ADMIN) {
                throw new CrossTenantAccessException();
            }
            assertAdminLimit(user.getTenant().getTenantId(), request.rol());
            user.setRol(request.rol());
        }
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        return MemberResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    public MemberResponseDTO updateStatus(Long id, UpdateMemberStatusRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        User user = findById(id);
        assertNotSelf(id);
        if (!currentIsSuperAdmin()) {
            if (user.getRol() == Rol.SUPER_ADMIN || user.getRol() == Rol.TENANT_ADMIN) {
                throw new InvalidMemberActionException("No puede modificar el estado de otro admin del grupo");
            }
        }
        user.setStatus(request.status());
        return MemberResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        User user = findById(id);
        assertNotSelf(id);
        if (!currentIsSuperAdmin()) {
            if (user.getRol() == Rol.SUPER_ADMIN || user.getRol() == Rol.TENANT_ADMIN) {
                throw new InvalidMemberActionException("No puede eliminar a otro admin del grupo");
            }
        }
        userRepository.delete(user);
    }

    private void assertAdminLimit(Long tenantId, Rol rol) {
        if (rol == Rol.TENANT_ADMIN) {
            long admins = userRepository.countByTenantTenantIdAndRol(tenantId, Rol.TENANT_ADMIN);
            if (admins >= MAX_ADMINS_PER_TENANT) {
                throw new InvalidMemberActionException(
                        "Cada grupo admite máximo " + MAX_ADMINS_PER_TENANT + " administradores"
                );
            }
        }
    }

    private Tenant resolveTenant(Long tenantId) {
        Long requested = currentIsSuperAdmin() ? tenantId : null;
        Long target = requested != null ? requested : currentTenantId();
        return tenantRepository.findById(target)
                .orElseThrow(() -> new TenantNotFoundException(target));
    }

    private User findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
        if (!currentIsSuperAdmin() && !user.getTenant().getTenantId().equals(currentTenantId())) {
            throw new CrossTenantAccessException();
        }
        return user;
    }

    private void assertNotSelf(Long id) {
        if (id.equals(currentUserId())) {
            throw new InvalidMemberActionException("No puede modificar su propia cuenta");
        }
    }

    private boolean currentIsSuperAdmin() {
        return currentPrincipal().getRoleId() == Rol.SUPER_ADMIN;
    }

    private Long currentTenantId() {
        return currentPrincipal().getTenantId();
    }

    private Long currentUserId() {
        return currentPrincipal().getUserId();
    }

    private UserDetailsImpl currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new RoleRequiredException(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        }
        return principal;
    }
}
