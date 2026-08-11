package org.glud.credentials;

import org.glud.credentials.attendance.model.Attendance;
import org.glud.credentials.attendance.repository.AttendanceRepository;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.event.dto.CreateEventRequestDTO;
import org.glud.credentials.event.dto.EventAttendanceResponseDTO;
import org.glud.credentials.event.dto.EventResponseDTO;
import org.glud.credentials.event.dto.EventStatus;
import org.glud.credentials.event.model.Event;
import org.glud.credentials.event.repository.EventRepository;
import org.glud.credentials.event.service.EventService;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.EventNotFoundException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
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
        event.setCreatedBy(user(1L, tenantId, "99999999999", Rol.SUPER_ADMIN));
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

    @Test
    void create_savesEventWithTenantChosenBySuperAdmin() {
        authenticate(1L, 5L, Rol.SUPER_ADMIN);
        when(tenantRepository.findById(3L)).thenReturn(Optional.of(tenant(3L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, 5L, "99999999999", Rol.SUPER_ADMIN)));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponseDTO dto = eventService.create(
                new CreateEventRequestDTO("  Asamblea GLUD  ", LocalDateTime.now().plusDays(30), 3L));

        assertEquals("Asamblea GLUD", dto.title());
        assertEquals(3L, dto.tenantId());
        assertEquals(EventStatus.SCHEDULED, dto.status());
        assertEquals(0L, dto.attendeesCount());
        verify(eventRepository).save(argThat(e -> e.getTenant().getTenantId() == 3L));
    }

    @Test
    void create_throwsWhenSuperAdminOmitsTenant() {
        authenticate(1L, 5L, Rol.SUPER_ADMIN);

        CreateEventRequestDTO request = new CreateEventRequestDTO("Asamblea", LocalDateTime.of(2026, 8, 10, 18, 0), null);
        assertThrows(InvalidMemberActionException.class, () -> eventService.create(request));
    }

    @Test
    void create_usesCurrentTenant_whenTenantAdmin() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(1L)));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, 1L, "20219999999", Rol.TENANT_ADMIN)));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.create(new CreateEventRequestDTO("Asamblea", LocalDateTime.of(2026, 8, 10, 18, 0), null));

        verify(eventRepository).save(argThat(e -> e.getTenant().getTenantId() == 1L));
    }

    @Test
    void findAll_superAdminSeesAllEvents() {
        authenticate(1L, 5L, Rol.SUPER_ADMIN);
        when(eventRepository.findAllByOrderByStartsAtDesc()).thenReturn(List.of(event(50L, 1L)));
        when(attendanceRepository.countByEventEventId(50L)).thenReturn(3L);

        List<EventResponseDTO> result = eventService.findAll();

        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).eventId());
        assertEquals(3L, result.get(0).attendeesCount());
        assertEquals("GLUD", result.get(0).tenantName());
    }

    @Test
    void findAll_tenantAdminSeesOnlyOwnTenant() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findByTenantTenantIdOrderByStartsAtDesc(1L)).thenReturn(List.of(event(50L, 1L)));
        when(attendanceRepository.countByEventEventId(50L)).thenReturn(0L);

        List<EventResponseDTO> result = eventService.findAll();

        assertEquals(1, result.size());
        verify(eventRepository).findByTenantTenantIdOrderByStartsAtDesc(1L);
    }

    @Test
    void findById_returnsEventWithAttendeeCount() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findById(50L)).thenReturn(Optional.of(event(50L, 1L)));
        when(attendanceRepository.countByEventEventId(50L)).thenReturn(2L);

        EventResponseDTO dto = eventService.findById(50L);

        assertEquals(50L, dto.eventId());
        assertEquals(2L, dto.attendeesCount());
        assertEquals(EventStatus.FINISHED, dto.status());
    }

    @Test
    void delete_removesEvent_whenNoAttendance() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findById(50L)).thenReturn(Optional.of(event(50L, 1L)));
        when(attendanceRepository.countByEventEventId(50L)).thenReturn(0L);

        eventService.delete(50L);

        verify(eventRepository).delete(argThat(e -> e.getEventId() == 50L));
    }

    @Test
    void delete_throwsConflict_whenAttendanceExists() {
        authenticate(9L, 1L, Rol.TENANT_ADMIN);
        when(eventRepository.findById(50L)).thenReturn(Optional.of(event(50L, 1L)));
        when(attendanceRepository.countByEventEventId(50L)).thenReturn(1L);

        assertThrows(InvalidMemberActionException.class, () -> eventService.delete(50L));
    }

    @Test
    void statusOf_returnsScheduled_whenEventInFuture() {
        Event future = event(1L, 1L);
        future.setStartsAt(LocalDateTime.now(ZoneId.of("America/Bogota")).plusDays(1));

        assertEquals(EventStatus.SCHEDULED, EventService.statusOf(future));
    }

    @Test
    void statusOf_returnsInProgress_withinWindow() {
        Event recent = event(1L, 1L);
        recent.setStartsAt(LocalDateTime.now(ZoneId.of("America/Bogota")).minusMinutes(30));

        assertEquals(EventStatus.IN_PROGRESS, EventService.statusOf(recent));
    }

    @Test
    void statusOf_returnsFinished_afterWindow() {
        Event old = event(1L, 1L);
        old.setStartsAt(LocalDateTime.now(ZoneId.of("America/Bogota")).minusHours(3));

        assertEquals(EventStatus.FINISHED, EventService.statusOf(old));
    }
}
