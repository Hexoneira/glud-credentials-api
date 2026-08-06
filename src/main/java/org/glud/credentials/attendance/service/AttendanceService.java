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
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.AttendanceAlreadyExistsException;
import org.glud.credentials.security.exception.CrossTenantAccessException;
import org.glud.credentials.security.exception.InvalidMemberActionException;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
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
    private final RoleGuard roleGuard;

    @Transactional
    public AttendanceResponseDTO registerAttendance(RegisterAttendanceRequestDTO request) {
        roleGuard.assertRole(Rol.TENANT_ADMIN, Rol.SUPER_ADMIN);

        String codigo = AttendanceCodeParser.extractCodigo(request.code());
        User member = userRepository.findByCodigo(codigo)
                .orElseThrow(() -> new MemberNotFoundException(codigo));

        if (!currentIsSuperAdmin() && !member.getTenant().getTenantId().equals(currentTenantId())) {
            throw new CrossTenantAccessException();
        }
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidMemberActionException("El miembro " + codigo + " está suspendido");
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
