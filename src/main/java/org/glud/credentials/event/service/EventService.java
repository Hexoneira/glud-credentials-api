package org.glud.credentials.event.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.attendance.repository.AttendanceRepository;
import org.glud.credentials.attendance.service.AttendanceCsvExporter;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.event.dto.CreateEventRequestDTO;
import org.glud.credentials.event.dto.EventAttendanceResponseDTO;
import org.glud.credentials.event.dto.EventResponseDTO;
import org.glud.credentials.event.dto.EventStatus;
import org.glud.credentials.event.model.Event;
import org.glud.credentials.event.repository.EventRepository;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.EventNotFoundException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    /** Ventana (en horas) en la que un evento se considera "en curso" desde su hora de inicio. */
    public static final long IN_PROGRESS_WINDOW_HOURS = 2;

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final EventRepository eventRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final RoleGuard roleGuard;

    @Transactional
    public EventResponseDTO create(CreateEventRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);

        Tenant tenant = resolveTenant(request.tenantId());
        User creator = userRepository.findById(currentUserId())
                .orElseThrow(() -> new MemberNotFoundException(currentUserId()));

        Event event = new Event();
        event.setTenant(tenant);
        event.setTitle(request.title().trim());
        event.setStartsAt(request.startsAt());
        event.setCreatedBy(creator);

        Event saved = eventRepository.save(event);
        return EventResponseDTO.from(saved, 0, statusOf(saved));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAll() {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);

        List<Event> events = currentIsSuperAdmin()
                ? eventRepository.findAllByOrderByStartsAtDesc()
                : eventRepository.findByTenantTenantIdOrderByStartsAtDesc(currentTenantId());

        return events.stream()
                .map(event -> EventResponseDTO.from(
                        event,
                        attendanceRepository.countByEventEventId(event.getEventId()),
                        statusOf(event)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long eventId) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        Event event = requireOwnedEvent(eventId);
        return EventResponseDTO.from(
                event,
                attendanceRepository.countByEventEventId(eventId),
                statusOf(event));
    }

    @Transactional(readOnly = true)
    public List<EventAttendanceResponseDTO> attendees(Long eventId) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        requireOwnedEvent(eventId);
        return findAttendees(eventId);
    }

    @Transactional(readOnly = true)
    public String exportAttendeesCsv(Long eventId) {
        Event event = requireOwnedEvent(eventId);
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        return AttendanceCsvExporter.attendeesCsv(event.getTitle(), findAttendees(eventId));
    }

    private List<EventAttendanceResponseDTO> findAttendees(Long eventId) {
        return attendanceRepository.findByEventEventIdOrderByCheckInAtDesc(eventId).stream()
                .map(EventAttendanceResponseDTO::from)
                .toList();
    }

    @Transactional
    public void delete(Long eventId) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);
        Event event = requireOwnedEvent(eventId);
        if (attendanceRepository.countByEventEventId(eventId) > 0) {
            throw new InvalidMemberActionException(
                    "No se puede eliminar un evento con asistencia registrada");
        }
        eventRepository.delete(event);
    }

    private Event requireOwnedEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!currentIsSuperAdmin() && !event.getTenant().getTenantId().equals(currentTenantId())) {
            throw new CrossTenantAccessException();
        }
        return event;
    }

    private Tenant resolveTenant(Long tenantId) {
        if (currentIsSuperAdmin()) {
            if (tenantId == null) {
                throw new InvalidMemberActionException("El super admin debe indicar el grupo del evento");
            }
            return tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new TenantNotFoundException(tenantId));
        }
        return tenantRepository.findById(currentTenantId())
                .orElseThrow(() -> new TenantNotFoundException(currentTenantId()));
    }

    public static EventStatus statusOf(Event event) {
        LocalDateTime now = LocalDateTime.now(COLOMBIA);
        LocalDateTime start = event.getStartsAt();
        if (now.isBefore(start)) {
            return EventStatus.SCHEDULED;
        }
        if (now.isBefore(start.plusHours(IN_PROGRESS_WINDOW_HOURS))) {
            return EventStatus.IN_PROGRESS;
        }
        return EventStatus.FINISHED;
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
