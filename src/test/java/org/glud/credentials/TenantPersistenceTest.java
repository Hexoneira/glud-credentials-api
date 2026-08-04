package org.glud.credentials;

import jakarta.persistence.EntityManager;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.TenantStatus;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TenantPersistenceTest {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsTenantAndUserWithForeignKeys() {
        Tenant tenant = new Tenant();
        tenant.setName("Grupo de Trabajo Test");
        tenant.setTenantCode("TEST");
        tenant.setDirector("Directora Test");
        tenant.setMemberLimit(50);
        tenant.setStatus(TenantStatus.SUSPENDED);
        entityManager.persist(tenant);
        entityManager.flush();

        User user = new User();
        user.setUsername("miembro-test");
        user.setPassword("hash-bcrypt");
        user.setCodigo("20210000001");
        user.setEmail("miembro@glud.org");
        user.setTenant(tenant);
        user.setRol(Rol.MIEMBRO);
        entityManager.persist(user);
        entityManager.flush();

        entityManager.clear();

        User loaded = userRepository.findById(user.getUserId()).orElseThrow();
        assertEquals("miembro-test", loaded.getUsername());
        assertEquals("20210000001", loaded.getCodigo());
        assertEquals("miembro@glud.org", loaded.getEmail());
        assertEquals(Rol.MIEMBRO, loaded.getRol());
        assertNotNull(loaded.getTenant());
        assertEquals("Grupo de Trabajo Test", loaded.getTenant().getName());
        assertEquals("TEST", loaded.getTenant().getTenantCode());
        assertEquals("Directora Test", loaded.getTenant().getDirector());
        assertEquals(50, loaded.getTenant().getMemberLimit());
        assertEquals(TenantStatus.SUSPENDED, loaded.getTenant().getStatus());
    }
}