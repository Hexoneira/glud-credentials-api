package org.glud.credentials;

import org.glud.credentials.attendance.dto.AttendanceResponseDTO;
import org.glud.credentials.attendance.dto.RegisterAttendanceRequestDTO;
import org.glud.credentials.attendance.model.Attendance;
import org.glud.credentials.attendance.repository.AttendanceRepository;
import org.glud.credentials.attendance.service.AttendanceService;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.AttendanceAlreadyExistsException;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
import org.glud.credentials.security.exception.InvalidScannedCodeException;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private UserRepository userRepository;

    private final RoleGuard roleGuard = new RoleGuard();

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(attendanceRepository, userRepository, roleGuard);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, Long tenantId, Rol rol) {
        UserDetailsImpl principal = new UserDetailsImpl(
                userId, "admin", "pw", tenantId, rol,
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

    private User user(Long id, Long tenantId, String codigo, Rol rol, UserStatus status) {
        User user = new User();
        user.setUserId(id);
        user.setCodigo(codigo);
        user.setUsername(codigo);
        user.setEmail("m@glud.org");
        user.setTenant(tenant(tenantId));
        user.setRol(rol);
        user.setStatus(status);
        return user;
    }

    private Attendance attendance(Long id, Long userId, Long tenantId, String codigo) {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(id);
        attendance.setUser(user(id, tenantId, codigo, Rol.MIEMBRO, UserStatus.ACTIVE));
        attendance.setTenant(tenant(tenantId));
        attendance.setMarkedBy(user(9L, tenantId, "20219999999", Rol.TENANT_ADMIN, UserStatus.ACTIVE));
        attendance.setCheckInAt(LocalDateTime.of(2026, 8, 5, 9, 30));
        return attendance;
    }

    @Test
    void register_registersMemberByPlainCodigo() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        User member = user(2L, 1L, "20210000002", Rol.MIEMBRO, UserStatus.ACTIVE);
        when(userRepository.findByCodigo("20210000002")).thenReturn(Optional.of(member));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, 1L, "20219999999", Rol.TENANT_ADMIN, UserStatus.ACTIVE)));
        when(attendanceRepository.existsByUserUserIdAndCheckInAtBetween(any(), any(), any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance saved = invocation.getArgument(0);
            saved.setAttendanceId(1L);
            return saved;
        });

        AttendanceResponseDTO result = attendanceService.registerAttendance(
                new RegisterAttendanceRequestDTO("20210000002"));

        assertEquals("20210000002", result.codigo());
        assertEquals(1L, result.tenantId());
        assertEquals("GLUD", result.tenantName());
        assertEquals("20219999999", result.markedByCodigo());
        verify(attendanceRepository).save(argThat(a -> a.getUser().getUserId().equals(2L)
                && a.getMarkedBy().getUserId().equals(9L)
                && a.getTenant().getTenantId().equals(1L)));
    }

    @Test
    void register_registersMemberFromCarnetQrPayload() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findByCodigo("20210000002")).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.MIEMBRO, UserStatus.ACTIVE)));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, 1L, "20219999999", Rol.TENANT_ADMIN, UserStatus.ACTIVE)));
        when(attendanceRepository.existsByUserUserIdAndCheckInAtBetween(any(), any(), any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponseDTO result = attendanceService.registerAttendance(
                new RegisterAttendanceRequestDTO("ID:20210000002|TOTP:123456"));

        assertEquals("20210000002", result.codigo());
    }

    @Test
    void register_throwsInvalidCode() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);

        assertThrows(InvalidScannedCodeException.class, () ->
                attendanceService.registerAttendance(new RegisterAttendanceRequestDTO("no válido")));
        verify(userRepository, never()).findByCodigo(any());
    }

    @Test
    void register_throwsNotFound_whenMemberMissing() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findByCodigo("20210000999")).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class, () ->
                attendanceService.registerAttendance(new RegisterAttendanceRequestDTO("20210000999")));
    }

    @Test
    void register_throwsCrossTenant_whenMemberBelongsToOtherTenant() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findByCodigo("20210000002")).thenReturn(Optional.of(user(2L, 2L, "20210000002", Rol.MIEMBRO, UserStatus.ACTIVE)));

        assertThrows(CrossTenantAccessException.class, () ->
                attendanceService.registerAttendance(new RegisterAttendanceRequestDTO("20210000002")));
    }

    @Test
    void register_throwsWhenMemberSuspended() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findByCodigo("20210000002")).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.MIEMBRO, UserStatus.SUSPENDED)));

        assertThrows(InvalidMemberActionException.class, () ->
                attendanceService.registerAttendance(new RegisterAttendanceRequestDTO("20210000002")));
    }

    @Test
    void register_throwsAlreadyExists_whenRegisteredToday() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(userRepository.findByCodigo("20210000002")).thenReturn(Optional.of(user(2L, 1L, "20210000002", Rol.MIEMBRO, UserStatus.ACTIVE)));
        when(attendanceRepository.existsByUserUserIdAndCheckInAtBetween(any(), any(), any())).thenReturn(true);

        assertThrows(AttendanceAlreadyExistsException.class, () ->
                attendanceService.registerAttendance(new RegisterAttendanceRequestDTO("20210000002")));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void register_throwsRoleRequired_whenMember() {
        authenticate(2L, 1L, Rol.MIEMBRO);

        assertThrows(RoleRequiredException.class, () ->
                attendanceService.registerAttendance(new RegisterAttendanceRequestDTO("20210000003")));
    }

    @Test
    void todayAttendance_listsOnlyOwnTenant() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(attendanceRepository.findByTenantTenantIdAndCheckInAtBetweenOrderByCheckInAtDesc(any(), any(), any()))
                .thenReturn(List.of(attendance(1L, 2L, 1L, "20210000002")));

        List<AttendanceResponseDTO> result = attendanceService.todayAttendance();

        assertEquals(1, result.size());
        assertEquals("20210000002", result.get(0).codigo());
        verify(attendanceRepository).findByTenantTenantIdAndCheckInAtBetweenOrderByCheckInAtDesc(
                eq(1L), any(), any());
    }

    @Test
    void todayAttendance_superAdminSeesAllTenants() {
        authenticate(1L, 1L, Rol.SUPER_ADMIN);
        when(attendanceRepository.findAllByCheckInAtBetweenOrderByCheckInAtDesc(any(), any()))
                .thenReturn(List.of(
                        attendance(1L, 2L, 1L, "20210000002"),
                        attendance(2L, 3L, 5L, "20210000003")));

        List<AttendanceResponseDTO> result = attendanceService.todayAttendance();

        assertEquals(2, result.size());
        assertEquals(5L, result.get(1).tenantId());
    }

    @Test
    void todayAttendance_throwsRoleRequired_whenMember() {
        authenticate(2L, 1L, Rol.MIEMBRO);

        assertThrows(RoleRequiredException.class, () -> attendanceService.todayAttendance());
    }
}
