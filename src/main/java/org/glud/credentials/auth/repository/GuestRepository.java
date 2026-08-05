package org.glud.credentials.auth.repository;

import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    long countByTenantTenantIdAndStatus(Long tenantId, GuestStatus status);

    boolean existsByCreatedByUserIdAndStatus(Long userId, GuestStatus status);

    Optional<Guest> findByCreatedByUserIdAndStatus(Long userId, GuestStatus status);

    Optional<Guest> findByCodigoAndTenantTenantId(String codigo, Long tenantId);

    Optional<Guest> findByAccessToken(String accessToken);

    List<Guest> findAllByCreatedByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
