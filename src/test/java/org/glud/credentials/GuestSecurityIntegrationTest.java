package org.glud.credentials;

import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.GuestRepository;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.components.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuestSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private GuestRepository guestRepository;

    private Tenant tenant1;

    @BeforeEach
    void setUp() {
        tenant1 = tenantRepository.findById(1L).orElseThrow();
    }

    private User createMember(String codigo) {
        User member = new User();
        member.setUsername(codigo);
        member.setCodigo(codigo);
        member.setPassword("$2a$10$irrelevant");
        member.setTenant(tenant1);
        member.setRol(Rol.MIEMBRO);
        return userRepository.save(member);
    }

    private void insertActiveGuest(String codigo, User creator) {
        Guest guest = new Guest();
        guest.setCodigo(codigo);
        guest.setName("Invitado " + codigo);
        guest.setTenant(tenant1);
        guest.setCreatedBy(creator);
        guest.setStatus(GuestStatus.ACTIVE);
        guestRepository.save(guest);
    }

    @Test
    @Transactional
    void guestsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": "101011000",
                                  "name": "Invitada Uno"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void member_createsGuest_returns201() throws Exception {
        User member = createMember("20210000040");
        String token = jwtUtils.generateJwtToken(member.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);

        mockMvc.perform(post("/api/guests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": "101011000",
                                  "name": "Invitada Uno",
                                  "email": "inv1@mail.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("101011000"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdByCodigo").value("20210000040"))
                .andExpect(jsonPath("$.tenantId").value(1));
    }

    @Test
    @Transactional
    void member_withActiveGuest_returns409() throws Exception {
        User member = createMember("20210000041");
        insertActiveGuest("101011000", member);
        String token = jwtUtils.generateJwtToken(member.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);

        mockMvc.perform(post("/api/guests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": "101011001",
                                  "name": "Invitado Dos"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El miembro ya tiene un invitado activo"));
    }

    @Test
    @Transactional
    void tenant_atLimit_returns403() throws Exception {
        User other = createMember("20210000042");
        for (int i = 1; i <= 15; i++) {
            insertActiveGuest(String.format(Locale.ROOT, "GUEST%04d", i), other);
        }

        User member = createMember("20210000043");
        String token = jwtUtils.generateJwtToken(member.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);

        mockMvc.perform(post("/api/guests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": "101011002",
                                  "name": "Invitado Tres"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Límite de invitados activos alcanzado (15)"));
    }
}
