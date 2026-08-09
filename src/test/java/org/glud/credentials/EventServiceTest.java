package org.glud.credentials;

import org.glud.credentials.attendance.model.Attendance;
import org.glud.credentials.attendance.repository.AttendanceRepository;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.event.dto.EventAttendanceResponseDTO;
import org.glud.credentials.event.model.Event;
import org.glud.credentials.event.repository.EventRepository;
import org.glud.credentials.event.service.EventService;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.EventNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    private final RoleGuard roleGuard = new RoleGuard();

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, tenantRepository, userRepository, attendanceRepository, roleGuard);
        org.springframework.test.util.ReflectionTestUtils.setField(eventService, "self", eventService);
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

    private User user(Long id, Long tenantId, String codigo, Rol rol) {
        User user = new User();
        user.setUserId(id);
        user.setCodigo(codigo);
        user.setUsername(codigo);
        user.setName("María Gómez");
        user.setTenant(tenant(tenantId));
        user.setRol(rol);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Event event(Long id, Long tenantId) {
        Event event = new Event();
        event.setEventId(id);
        event.setTitle("Asamblea GLUD");
        event.setStartsAt(LocalDateTime.of(2026, 8, 5, 18, 0));
        event.setTenant(tenant(tenantId));
        return event;
    }

    private Attendance attendance(Long id, Long userId, Long tenantId, String codigo) {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(id);
        attendance.setUser(user(userId, tenantId, codigo, Rol.MIEMBRO));
        attendance.setTenant(tenant(tenantId));
        attendance.setMarkedBy(user(9L, tenantId, "20219999999", Rol.TENANT_ADMIN));
        attendance.setCheckInAt(LocalDateTime.of(2026, 8, 5, 18, 5));
        return attendance;
    }

    @Test
    void attendees_listsMembersOfOwnEvent() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findById(50L)).thenReturn(Optional.of(event(50L, 1L)));
        when(attendanceRepository.findByEventEventIdOrderByCheckInAtDesc(50L))
                .thenReturn(List.of(attendance(1L, 2L, 1L, "20210000002")));

        List<EventAttendanceResponseDTO> result = eventService.attendees(50L);

        assertEquals(1, result.size());
        assertEquals("20210000002", result.get(0).codigo());
        assertEquals("María Gómez", result.get(0).name());
    }

    @Test
    void attendees_throwsCrossTenant_whenEventOfOtherTenant() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findById(50L)).thenReturn(Optional.of(event(50L, 2L)));

        assertThrows(CrossTenantAccessException.class, () -> eventService.attendees(50L));
    }

    @Test
    void attendees_throwsNotFound_whenEventMissing() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventService.attendees(50L));
    }

    @Test
    void exportAttendeesCsv_includesTitleAndRows() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findById(50L)).thenReturn(Optional.of(event(50L, 1L)));
        when(attendanceRepository.findByEventEventIdOrderByCheckInAtDesc(50L))
                .thenReturn(List.of(
                        attendance(1L, 2L, 1L, "20210000002"),
                        attendance(2L, 3L, 1L, "20210000003")));

        String csv = eventService.exportAttendeesCsv(50L);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Asamblea GLUD"));
        assertTrue(csv.contains("20210000002"));
        assertTrue(csv.contains("20210000003"));
        assertTrue(csv.contains("María Gómez"));
        assertTrue(csv.contains("Registrado por"));
    }

    @Test
    void exportAttendeesCsv_throwsRoleRequired_whenMember() {
        authenticate(2L, 1L, Rol.MIEMBRO);
        when(eventRepository.findById(50L)).thenReturn(Optional.of(event(50L, 1L)));

        assertThrows(RoleRequiredException.class, () -> eventService.exportAttendeesCsv(50L));
    }
}
