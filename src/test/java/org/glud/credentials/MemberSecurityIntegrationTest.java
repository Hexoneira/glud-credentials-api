package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.TenantStatus;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.components.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberSecurityIntegrationTest {

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

    private Long tenant2Id;
    private Long memberOfTenant2;

    @BeforeEach
    void setUp() {
        Tenant tenant2 = new Tenant();
        tenant2.setName("Grupo 2");
        tenant2.setTenantCode("GRUPO2");
        tenant2.setDirector("Directora 2");
        tenant2.setMemberLimit(50);
        tenant2.setStatus(TenantStatus.ACTIVE);
        tenant2 = tenantRepository.save(tenant2);
        tenant2Id = tenant2.getTenantId();

        User member2 = new User();
        member2.setUsername("20210000010");
        member2.setCodigo("20210000010");
        member2.setPassword(passwordEncoder.encode("clave123"));
        member2.setTenant(tenant2);
        member2.setRol(Rol.MIEMBRO);
        memberOfTenant2 = userRepository.save(member2).getUserId();
    }

    @Test
    @Transactional
    void membersEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void membersEndpoint_allowsSuperAdmin() throws Exception {
        String token = jwtUtils.generateJwtToken(1L, 1L, Rol.SUPER_ADMIN);

        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void membersEndpoint_deniesMemberRole() throws Exception {
        String token = jwtUtils.generateJwtToken(memberOfTenant2, tenant2Id, Rol.MIEMBRO);

        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @Transactional
    void memberUpdate_deniedWhenMemberBelongsToOtherTenant() throws Exception {
        Tenant adminTenant = tenantRepository.findById(1L).orElseThrow();
        User admin = new User();
        admin.setUsername("20210000099");
        admin.setCodigo("20210000099");
        admin.setPassword(passwordEncoder.encode("clave123"));
        admin.setTenant(adminTenant);
        admin.setRol(Rol.TENANT_ADMIN);
        admin = userRepository.save(admin);

        String token = jwtUtils.generateJwtToken(admin.getUserId(), 1L, Rol.TENANT_ADMIN);

        mockMvc.perform(patch("/api/members/" + memberOfTenant2 + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "SUSPENDED"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.codigo == '20210000010')]").isEmpty())
                .andExpect(jsonPath("$[?(@.codigo == '20210000099')]").isNotEmpty());
    }
}
