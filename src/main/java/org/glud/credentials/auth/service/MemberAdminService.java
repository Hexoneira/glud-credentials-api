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

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleGuard roleGuard;

    @Transactional(readOnly = true)
    public List<MemberResponseDTO> findAll() {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        return userRepository.findByTenantTenantId(currentTenantId()).stream()
                .map(MemberResponseDTO::from)
                .toList();
    }

    @Transactional
    public MemberResponseDTO create(CreateMemberRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        if (request.rol() == Rol.SUPER_ADMIN) {
            throw new InvalidMemberActionException("No se puede asignar el rol SUPER_ADMIN a un miembro");
        }
        if (userRepository.existsByCodigo(request.codigo())) {
            throw new MemberAlreadyExistsException("Ya existe un miembro con el código " + request.codigo());
        }

        Tenant tenant = tenantRepository.findById(currentTenantId())
                .orElseThrow(() -> new TenantNotFoundException(currentTenantId()));

        User user = new User();
        user.setCodigo(request.codigo());
        user.setUsername(request.codigo());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRol(request.rol());
        user.setStatus(UserStatus.ACTIVE);
        user.setTenant(tenant);

        return MemberResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    public MemberResponseDTO update(Long id, UpdateMemberRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        User user = findInTenant(id);
        if (request.rol() != null) {
            if (request.rol() == Rol.SUPER_ADMIN) {
                throw new InvalidMemberActionException("No se puede asignar el rol SUPER_ADMIN a un miembro");
            }
            user.setRol(request.rol());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        return MemberResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    public MemberResponseDTO updateStatus(Long id, UpdateMemberStatusRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        User user = findInTenant(id);
        assertNotSelf(id);
        user.setStatus(request.status());
        return MemberResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        User user = findInTenant(id);
        assertNotSelf(id);
        userRepository.delete(user);
    }

    private void assertNotSelf(Long id) {
        if (id.equals(currentUserId())) {
            throw new InvalidMemberActionException("No puede modificar su propia cuenta");
        }
    }

    private User findInTenant(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
        if (!user.getTenant().getTenantId().equals(currentTenantId())) {
            throw new CrossTenantAccessException();
        }
        return user;
    }

    private Long currentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new RoleRequiredException(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        }
        return principal.getTenantId();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new RoleRequiredException(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        }
        return principal.getUserId();
    }
}
