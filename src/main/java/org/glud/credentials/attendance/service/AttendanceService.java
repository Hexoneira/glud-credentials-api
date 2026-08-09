package org.glud.credentials.attendance.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.attendance.dto.AttendanceResponseDTO;
import org.glud.credentials.attendance.dto.RegisterAttendanceRequestDTO;
import org.glud.credentials.attendance.model.Attendance;
import org.glud.credentials.attendance.repository.AttendanceRepository;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.event.model.Event;
import org.glud.credentials.event.repository.EventRepository;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.AttendanceAlreadyExistsException;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.EventNotFoundException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
import org.glud.credentials.security.exception.InvalidScannedCodeException;
import org.glud.credentials.security.exception.InvalidTOTPException;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RoleGuard roleGuard;
    private final TOTPService totpService;

    @Transactional
    public AttendanceResponseDTO registerAttendance(RegisterAttendanceRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);

        String scanned = request.code();
        String codigo = AttendanceCodeParser.extractCodigo(scanned);
        User member = userRepository.findByCodigo(codigo)
                .orElseThrow(() -> new MemberNotFoundException(codigo));

        if (!currentIsSuperAdmin() && !member.getTenant().getTenantId().equals(currentTenantId())) {
            throw new CrossTenantAccessException();
        }
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidMemberActionException("El miembro " + codigo + " está suspendido");
        }

        // Si el carnet digital trae TOTP, se valida contra la semilla derivada
        // del servidor (SHA-256 de codigo:tenantCode:serverSecret)
        String totp = AttendanceCodeParser.extractTotp(scanned);
        if (totp != null) {
            assertValidTotp(member, totp);
        }

        Long eventId = request.eventId();
        if (eventId != null) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EventNotFoundException(eventId));
            if (!currentIsSuperAdmin() && !event.getTenant().getTenantId().equals(currentTenantId())) {
                throw new CrossTenantAccessException();
            }
            if (attendanceRepository.existsByEventEventIdAndUserUserId(eventId, member.getUserId())) {
                throw new AttendanceAlreadyExistsException(codigo, event.getTitle());
            }
            User marker = userRepository.findById(currentUserId())
                    .orElseThrow(() -> new MemberNotFoundException(currentUserId()));

            Attendance attendance = new Attendance();
            attendance.setTenant(member.getTenant());
            attendance.setUser(member);
            attendance.setMarkedBy(marker);
            attendance.setEvent(event);
            attendance.setCheckInAt(LocalDateTime.now());

            return AttendanceResponseDTO.from(attendanceRepository.save(attendance));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        if (attendanceRepository.existsByUserUserIdAndCheckInAtBetween(
                member.getUserId(), startOfDay, startOfDay.plusDays(1))) {
            throw new AttendanceAlreadyExistsException(codigo);
        }

        User marker = userRepository.findById(currentUserId())
                .orElseThrow(() -> new MemberNotFoundException(currentUserId()));

        Attendance attendance = new Attendance();
        attendance.setTenant(member.getTenant());
        attendance.setUser(member);
        attendance.setMarkedBy(marker);
        attendance.setCheckInAt(now);

        return AttendanceResponseDTO.from(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> todayAttendance() {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Attendance> records = currentIsSuperAdmin()
                ? attendanceRepository.findAllByCheckInAtBetweenOrderByCheckInAtDesc(startOfDay, endOfDay)
                : attendanceRepository.findByTenantTenantIdAndCheckInAtBetweenOrderByCheckInAtDesc(
                        currentTenantId(), startOfDay, endOfDay);

        return records.stream().map(AttendanceResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public String exportTodayCsv() {
        return AttendanceCsvExporter.todayCsv(todayAttendance());
    }

    private void assertValidTotp(User member, String totp) {
        try {
            String seed = totpService.generateSeed(member.getCodigo(), member.getTenant().getTenantCode());
            if (!totpService.verify(seed, totp)) {
                throw new InvalidTOTPException();
            }
        } catch (InvalidTOTPException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidTOTPException();
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
