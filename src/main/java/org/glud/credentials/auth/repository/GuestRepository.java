package org.glud.credentials.auth.repository;

import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    long countByTenantTenantIdAndStatus(Long tenantId, GuestStatus status);

    boolean existsByCreatedByUserIdAndStatus(Long userId, GuestStatus status);

    Optional<Guest> findByCodigoAndTenantTenantId(String codigo, Long tenantId);
}
