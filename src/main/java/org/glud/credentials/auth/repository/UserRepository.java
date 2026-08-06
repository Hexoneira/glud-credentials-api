package org.glud.credentials.auth.repository;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    long countByTenantTenantId(Long tenantId);

    long countByTenantTenantIdAndRol(Long tenantId, Rol rol);

    List<User> findByTenantTenantId(Long tenantId);
}
