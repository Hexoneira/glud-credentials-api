package org.glud.credentials.auth.repository;

import org.glud.credentials.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    void findByUsername(String username);
}
