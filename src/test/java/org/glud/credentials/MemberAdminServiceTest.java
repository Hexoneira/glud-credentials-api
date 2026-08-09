package org.glud.credentials;

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
import org.glud.credentials.auth.service.MemberAdminService;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
import org.glud.credentials.security.exception.MemberAlreadyExistsException;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberAdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberAdminService memberAdminService;

    private final RoleGuard roleGuard = new RoleGuard();

    @BeforeEach
    void setUp() {
        memberAdminService = new MemberAdminService(userRepository, tenantRepository, passwordEncoder, roleGuard);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, Long tenantId, Rol rol) {
        UserDetailsImpl principal = new UserDetailsImpl(
                userId, "usuario", "pw", tenantId, rol,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Tenant tenant(Long id) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(id);
        tenant.setName("GLUD");
        return tenant;
    }

    private User user(Long id, Long tenantId, String codigo, Rol rol) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setName("Grupo " + tenantId);
        User user = new User();
        user.setUserId(id);
        user.setUsername(codigo);
        user.setCodigo(codigo);
        user.setTenant(tenant);
        user.setRol(rol);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void findAll_listsOnlyMembersOfCurrentTenant() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findByTenantTenantId(1L))
                .thenReturn(List.of(user(2L, 1L, "20210000002", Rol.MIEMBRO)));

        List<MemberResponseDTO> result = memberAdminService.findAll(null);

        assertEquals(1, result.size());
        assertEquals("20210000002", result.get(0).codigo());
        assertEquals(1L, result.get(0).tenantId());
        verify(userRepository).findByTenantTenantId(1L);
    }

    @Test
    void findAll_throwsRoleRequired_whenNotAdmin() {
        authenticate(2L, 1L, Rol.MIEMBRO);

        assertThrows(RoleRequiredException.class, () -> memberAdminService.findAll(null));
        verify(userRepository, never()).findByTenantTenantId(any());
    }

    @Test
    void create_createsMemberInCurrentTenant() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.existsByCodigo("20210000002")).thenReturn(false);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(1L)));
        when(passwordEncoder.encode("clave123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(2L);
            return saved;
        });

        CreateMemberRequestDTO request = new CreateMemberRequestDTO("20210000002", "clave123", "María Gómez", Rol.MIEMBRO, null);

        MemberResponseDTO result = memberAdminService.create(request);

        assertEquals(2L, result.id());
        assertEquals("20210000002", result.codigo());
        assertEquals("María Gómez", result.name());
        assertEquals(Rol.MIEMBRO, result.rol());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(1L, result.tenantId());
        verify(userRepository).save(argThat(u ->
                "ENCODED".equals(u.getPassword()) && u.getTenant().getTenantId().equals(1L)));
    }

    @Test
    void create_throwsAlreadyExists_whenCodigoExists() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.existsByCodigo("20210000002")).thenReturn(true);

        CreateMemberRequestDTO request = new CreateMemberRequestDTO("20210000002", "clave123", "Juan Pérez", Rol.MIEMBRO, null);

        assertThrows(MemberAlreadyExistsException.class, () -> memberAdminService.create(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_throwsInvalidAction_whenAssigningSuperAdmin() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);

        CreateMemberRequestDTO request = new CreateMemberRequestDTO("20210000002", "clave123", "Juan Pérez", Rol.SUPER_ADMIN, null);

        assertThrows(InvalidMemberActionException.class, () -> memberAdminService.create(request));
    }

    @Test
    void update_updatesEmailAndRol() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.MIEMBRO)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMemberRequestDTO request = new UpdateMemberRequestDTO("Nombre Nuevo", Rol.INVITADO);

        MemberResponseDTO result = memberAdminService.update(2L, request);

        assertEquals("Nombre Nuevo", result.name());
        assertEquals(Rol.INVITADO, result.rol());
    }

    @Test
    void update_throwsCrossTenant_whenMemberBelongsToOtherTenant() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 2L, "20210000002", Rol.MIEMBRO)));

        UpdateMemberRequestDTO request = new UpdateMemberRequestDTO("Nombre X", null);

        assertThrows(CrossTenantAccessException.class, () -> memberAdminService.update(2L, request));
    }

    @Test
    void update_throwsNotFound_whenMemberMissing() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateMemberRequestDTO request = new UpdateMemberRequestDTO("Nombre X", null);

        assertThrows(MemberNotFoundException.class, () -> memberAdminService.update(99L, request));
    }

    @Test
    void updateStatus_suspendsMember() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.MIEMBRO)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMemberStatusRequestDTO request = new UpdateMemberStatusRequestDTO(UserStatus.SUSPENDED);

        MemberResponseDTO result = memberAdminService.updateStatus(2L, request);

        assertEquals(UserStatus.SUSPENDED, result.status());
    }

    @Test
    void updateStatus_throwsSelfModification() {
        authenticate(2L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.TENANT_ADMIN)));

        UpdateMemberStatusRequestDTO request = new UpdateMemberStatusRequestDTO(UserStatus.SUSPENDED);

        assertThrows(InvalidMemberActionException.class, () -> memberAdminService.updateStatus(2L, request));
    }

    @Test
    void delete_deletesMemberInTenant() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        User member = user(2L, 1L, "20210000002", Rol.MIEMBRO);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));

        memberAdminService.delete(2L);

        verify(userRepository).delete(member);
    }

    @Test
    void delete_throwsCrossTenant_whenMemberBelongsToOtherTenant() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 2L, "20210000002", Rol.MIEMBRO)));

        assertThrows(CrossTenantAccessException.class, () -> memberAdminService.delete(2L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_throwsSelfModification() {
        authenticate(2L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.TENANT_ADMIN)));

        assertThrows(InvalidMemberActionException.class, () -> memberAdminService.delete(2L));
    }

    @Test
    void create_throwsTenantNotFound_whenTenantMissing() {
        authenticate(10L, 99L, Rol.TENANT_ADMIN);
        when(userRepository.existsByCodigo("20210000002")).thenReturn(false);
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        CreateMemberRequestDTO request = new CreateMemberRequestDTO("20210000002", "clave123", "Juan Pérez", Rol.MIEMBRO, null);

        assertThrows(TenantNotFoundException.class, () -> memberAdminService.create(request));
    }

    @Test
    void findAll_superAdmin_filtersByTenantId() {
        authenticate(10L, 1L, Rol.SUPER_ADMIN);
        when(userRepository.findByTenantTenantId(5L))
                .thenReturn(List.of(user(7L, 5L, "20210000007", Rol.MIEMBRO)));

        List<MemberResponseDTO> result = memberAdminService.findAll(5L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).tenantId());
        verify(userRepository).findByTenantTenantId(5L);
    }

    @Test
    void findAll_superAdmin_listsAllTenants_whenNoFilter() {
        authenticate(10L, 1L, Rol.SUPER_ADMIN);
        when(userRepository.findAll())
                .thenReturn(List.of(user(7L, 5L, "20210000007", Rol.MIEMBRO)));

        List<MemberResponseDTO> result = memberAdminService.findAll(null);

        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void create_superAdmin_createsInAnotherTenant() {
        authenticate(10L, 1L, Rol.SUPER_ADMIN);
        when(userRepository.existsByCodigo("20210000009")).thenReturn(false);
        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant(5L)));
        when(passwordEncoder.encode("clave123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(9L);
            return saved;
        });

        CreateMemberRequestDTO request = new CreateMemberRequestDTO("20210000009", "clave123", "Rol M", Rol.MIEMBRO, 5L);

        MemberResponseDTO result = memberAdminService.create(request);

        assertEquals(9L, result.id());
        assertEquals(5L, result.tenantId());
        verify(userRepository).save(argThat(u -> u.getTenant().getTenantId().equals(5L)));
    }

    @Test
    void create_superAdmin_grantsSuperAdminRole() {
        authenticate(10L, 1L, Rol.SUPER_ADMIN);
        when(userRepository.existsByCodigo("20210000008")).thenReturn(false);
        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant(5L)));
        when(passwordEncoder.encode("clave123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateMemberRequestDTO request = new CreateMemberRequestDTO("20210000008", "clave123", "S A", Rol.SUPER_ADMIN, 5L);

        MemberResponseDTO result = memberAdminService.create(request);

        assertEquals(Rol.SUPER_ADMIN, result.rol());
    }

    @Test
    void create_throwsAdminLimit_whenTenantAlreadyHasTwoAdmins() {
        authenticate(10L, 1L, Rol.SUPER_ADMIN);
        when(userRepository.existsByCodigo("20210000003")).thenReturn(false);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(1L)));
        when(userRepository.countByTenantTenantIdAndRol(1L, Rol.TENANT_ADMIN)).thenReturn(2L);

        CreateMemberRequestDTO request = new CreateMemberRequestDTO("20210000003", "clave123", "T A", Rol.TENANT_ADMIN, 1L);

        InvalidMemberActionException ex = assertThrows(InvalidMemberActionException.class,
                () -> memberAdminService.create(request));
        assertTrue(ex.getMessage().contains("máximo 2"));
    }

    @Test
    void update_tenantAdmin_cannotTouchSuperAdmin() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.SUPER_ADMIN)));

        UpdateMemberRequestDTO request = new UpdateMemberRequestDTO("Nombre X", Rol.MIEMBRO);

        assertThrows(CrossTenantAccessException.class, () -> memberAdminService.update(2L, request));
    }

    @Test
    void update_superAdmin_grantsSuperAdminToMember() {
        authenticate(10L, 1L, Rol.SUPER_ADMIN);
        User target = user(2L, 1L, "20210000002", Rol.MIEMBRO);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMemberRequestDTO request = new UpdateMemberRequestDTO(null, Rol.SUPER_ADMIN);

        MemberResponseDTO result = memberAdminService.update(2L, request);

        assertEquals(Rol.SUPER_ADMIN, result.rol());
    }

    @Test
    void update_throwsSelfRoleChange() {
        authenticate(2L, 1L, Rol.SUPER_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.SUPER_ADMIN)));

        UpdateMemberRequestDTO request = new UpdateMemberRequestDTO(null, Rol.MIEMBRO);

        assertThrows(InvalidMemberActionException.class, () -> memberAdminService.update(2L, request));
    }

    @Test
    void update_promotionThrowsAdminLimit_whenTwoAdminsExist() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.MIEMBRO)));
        when(userRepository.countByTenantTenantIdAndRol(1L, Rol.TENANT_ADMIN)).thenReturn(2L);

        UpdateMemberRequestDTO request = new UpdateMemberRequestDTO(null, Rol.TENANT_ADMIN);

        assertThrows(InvalidMemberActionException.class, () -> memberAdminService.update(2L, request));
    }

    @Test
    void delete_tenantAdmin_cannotDeleteAnotherAdmin() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.TENANT_ADMIN)));

        assertThrows(InvalidMemberActionException.class, () -> memberAdminService.delete(2L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void updateStatus_tenantAdmin_cannotSuspendAnotherAdmin() {
        authenticate(10L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.TENANT_ADMIN)));

        UpdateMemberStatusRequestDTO request = new UpdateMemberStatusRequestDTO(UserStatus.SUSPENDED);

        assertThrows(InvalidMemberActionException.class, () -> memberAdminService.updateStatus(2L, request));
    }

    @Test
    void updateStatus_superAdmin_canSuspendAnyMember() {
        authenticate(10L, 1L, Rol.SUPER_ADMIN);
        User admin = user(2L, 5L, "20210000002", Rol.TENANT_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMemberStatusRequestDTO request = new UpdateMemberStatusRequestDTO(UserStatus.SUSPENDED);

        MemberResponseDTO result = memberAdminService.updateStatus(2L, request);

        assertEquals(UserStatus.SUSPENDED, result.status());
        assertEquals(5L, result.tenantId());
    }
}
