package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.components.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void tenantsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void tenantsEndpoint_allowsSuperAdmin() throws Exception {
        String token = jwtUtils.generateJwtToken(1L, 1L, Rol.SUPER_ADMIN);

        mockMvc.perform(get("/api/tenants").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void tenantsEndpoint_deniesMemberRole() throws Exception {
        Tenant tenant = tenantRepository.findById(1L).orElseThrow();
        User member = new User();
        member.setUsername("20210000001");
        member.setCodigo("20210000001");
        member.setPassword(passwordEncoder.encode("clave123"));
        member.setTenant(tenant);
        member.setRol(Rol.MIEMBRO);
        member = userRepository.save(member);

        String token = jwtUtils.generateJwtToken(member.getUserId(), tenant.getTenantId(), Rol.MIEMBRO);

        mockMvc.perform(get("/api/tenants").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
