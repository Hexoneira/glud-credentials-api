package org.glud.credentials;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrations_createsSeededTenantsAndUsers() {
        Integer tenants = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class);
        Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

        assertEquals(1, tenants);
        assertEquals(1, users);
    }

    @Test
    void seed_createsSuperAdminWithValidPassword() {
        String storedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = '20210000000'", String.class);
        String rol = jdbcTemplate.queryForObject(
                "SELECT rol FROM users WHERE username = '20210000000'", String.class);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM tenants WHERE tenant_code = 'GLUD'", String.class);

        assertEquals("SUPER_ADMIN", rol);
        assertEquals("ACTIVE", status);
        assertTrue(new BCryptPasswordEncoder().matches("admin123", storedPassword));
    }
}
