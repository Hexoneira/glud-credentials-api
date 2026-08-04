package org.glud.credentials;

import org.glud.credentials.auth.dto.CreateTenantRequestDTO;
import org.glud.credentials.auth.dto.TenantResponseDTO;
import org.glud.credentials.auth.dto.UpdateTenantRequestDTO;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.TenantStatus;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.auth.service.TenantService;
import org.glud.credentials.security.authorization.SuperAdminGuard;
import org.glud.credentials.security.exception.TenantAlreadyExistsException;
import org.glud.credentials.security.exception.TenantHasMembersException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SuperAdminGuard superAdminGuard;

    @InjectMocks
    private TenantService tenantService;

    private Tenant tenant(Long id, TenantStatus status) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(id);
        tenant.setName("GLUD");
        tenant.setTenantCode("GLUD");
        tenant.setDirector("Directora GLUD");
        tenant.setMemberLimit(100);
        tenant.setStatus(status);
        return tenant;
    }

    @Test
    void findAll_returnsAllTenantsWithCurrentMembers() {
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(1L, TenantStatus.ACTIVE)));
        when(userRepository.countByTenantTenantId(1L)).thenReturn(3L);

        List<TenantResponseDTO> result = tenantService.findAll();

        assertEquals(1, result.size());
        TenantResponseDTO dto = result.get(0);
        assertEquals(1L, dto.id());
        assertEquals("GLUD", dto.name());
        assertEquals(3L, dto.currentMembers());
        assertEquals(TenantStatus.ACTIVE, dto.status());
        verify(superAdminGuard).assertSuperAdmin();
    }

    @Test
    void create_createsAndReturnsTenant() {
        when(tenantRepository.existsByTenantCode("NVO")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant saved = invocation.getArgument(0);
            saved.setTenantId(7L);
            return saved;
        });

        CreateTenantRequestDTO request = new CreateTenantRequestDTO("Nuevo Grupo", "NVO", "Nueva Directora", 50);

        TenantResponseDTO result = tenantService.create(request);

        assertEquals(7L, result.id());
        assertEquals("Nuevo Grupo", result.name());
        assertEquals("NVO", result.tenantCode());
        assertEquals("Nueva Directora", result.director());
        assertEquals(50, result.memberLimit());
        assertEquals(TenantStatus.ACTIVE, result.status());
        verify(superAdminGuard).assertSuperAdmin();
    }

    @Test
    void create_throwsAlreadyExists_whenTenantCodeExists() {
        when(tenantRepository.existsByTenantCode("NVO")).thenReturn(true);

        CreateTenantRequestDTO request = new CreateTenantRequestDTO("Nuevo Grupo", "NVO", "Directora", 50);

        assertThrows(TenantAlreadyExistsException.class, () -> tenantService.create(request));
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void update_updatesOnlyProvidedFields() {
        Tenant tenant = tenant(1L, TenantStatus.ACTIVE);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTenantRequestDTO request = new UpdateTenantRequestDTO("Actualizado", null, 120);

        TenantResponseDTO result = tenantService.update(1L, request);

        assertEquals("Actualizado", result.name());
        assertEquals(120, result.memberLimit());
        assertEquals("Directora GLUD", result.director());
        assertEquals("GLUD", result.tenantCode());
        verify(superAdminGuard).assertSuperAdmin();
    }

    @Test
    void update_updatesDirectorWhenProvided() {
        Tenant tenant = tenant(1L, TenantStatus.ACTIVE);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTenantRequestDTO request = new UpdateTenantRequestDTO(null, "Nueva Directora", null);

        TenantResponseDTO result = tenantService.update(1L, request);

        assertEquals("Nueva Directora", result.director());
        assertEquals("GLUD", result.name());
        assertEquals(100, result.memberLimit());
        verify(superAdminGuard).assertSuperAdmin();
    }

    @Test
    void update_throwsNotFound_whenTenantMissing() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        UpdateTenantRequestDTO request = new UpdateTenantRequestDTO("Actualizado", "Directora", 100);

        assertThrows(TenantNotFoundException.class, () -> tenantService.update(1L, request));
    }

    @Test
    void suspend_suspendsActiveTenant() {
        Tenant tenant = tenant(1L, TenantStatus.ACTIVE);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantResponseDTO result = tenantService.suspend(1L);

        assertEquals(TenantStatus.SUSPENDED, result.status());
        verify(superAdminGuard).assertSuperAdmin();
    }

    @Test
    void suspend_returnsSame_whenAlreadySuspended() {
        Tenant tenant = tenant(1L, TenantStatus.SUSPENDED);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        TenantResponseDTO result = tenantService.suspend(1L);

        assertEquals(TenantStatus.SUSPENDED, result.status());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void reactivate_reactivatesSuspendedTenant() {
        Tenant tenant = tenant(1L, TenantStatus.SUSPENDED);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantResponseDTO result = tenantService.reactivate(1L);

        assertEquals(TenantStatus.ACTIVE, result.status());
        verify(superAdminGuard).assertSuperAdmin();
    }

    @Test
    void reactivate_returnsSame_whenAlreadyActive() {
        Tenant tenant = tenant(1L, TenantStatus.ACTIVE);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        TenantResponseDTO result = tenantService.reactivate(1L);

        assertEquals(TenantStatus.ACTIVE, result.status());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void delete_deletesTenantWithoutMembers() {
        Tenant tenant = tenant(1L, TenantStatus.ACTIVE);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(userRepository.countByTenantTenantId(1L)).thenReturn(0L);

        tenantService.delete(1L);

        verify(tenantRepository).delete(tenant);
        verify(superAdminGuard).assertSuperAdmin();
    }

    @Test
    void delete_throwsHasMembers_whenTenantHasUsers() {
        Tenant tenant = tenant(1L, TenantStatus.ACTIVE);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(userRepository.countByTenantTenantId(1L)).thenReturn(2L);

        assertThrows(TenantHasMembersException.class, () -> tenantService.delete(1L));
        verify(tenantRepository, never()).delete(any());
    }

    @Test
    void delete_throwsNotFound_whenTenantMissing() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TenantNotFoundException.class, () -> tenantService.delete(1L));
    }
}
