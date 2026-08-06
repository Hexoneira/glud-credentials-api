package org.glud.credentials.attendance.repository;

import org.glud.credentials.attendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByUserUserIdAndCheckInAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<Attendance> findByTenantTenantIdAndCheckInAtBetweenOrderByCheckInAtDesc(
            Long tenantId, LocalDateTime start, LocalDateTime end);

    List<Attendance> findAllByCheckInAtBetweenOrderByCheckInAtDesc(LocalDateTime start, LocalDateTime end);
}
