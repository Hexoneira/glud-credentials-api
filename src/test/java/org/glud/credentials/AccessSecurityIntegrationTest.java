package org.glud.credentials;

import org.glud.credentials.access.model.AccessLog;
import org.glud.credentials.access.model.AccessResult;
import org.glud.credentials.access.repository.AccessLogRepository;
import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.GuestRepository;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccessSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TOTPService totpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AccessLogRepository accessLogRepository;

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

    private Guest createGuest(String codigo, User creator, GuestStatus status) {
        Guest guest = new Guest();
        guest.setCodigo(codigo);
        guest.setName("Invitado " + codigo);
        guest.setTenant(tenant1);
        guest.setCreatedBy(creator);
        guest.setStatus(status);
        return guestRepository.save(guest);
    }

    private String currentCode(String codigo) throws NoSuchAlgorithmException, InvalidKeyException {
        String seed = totpService.generateSeed(codigo, tenant1.getTenantCode());
        long timeIndex = Instant.now().getEpochSecond() / 30;
        return totpService.generateCode(seed, timeIndex);
    }

    private boolean hasLogFor(Long subjectId, AccessResult result) {
        return accessLogRepository.findAll().stream()
                .anyMatch(log -> subjectId.equals(log.getSubjectId()) && log.getResult() == result);
    }

    @Test
    @Transactional
    void validate_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "totpCode": "123456"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void validate_allowsMemberWithRealTotp() throws Exception {
        User member = createMember("20210000050");
        String token = jwtUtils.generateJwtToken(member.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);
        String code = currentCode(member.getCodigo());

        mockMvc.perform(post("/api/access/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "totpCode": "%s",
                                  "deviceId": "escanner-01",
                                  "location": "Puerta principal"
                                }
                                """.formatted(member.getUserId(), code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));

        assertTrue(hasLogFor(member.getUserId(), AccessResult.ALLOWED));
    }

    @Test
    @Transactional
    void validate_deniesWrongTotpAndLogsFailedAttempt() throws Exception {
        User member = createMember("20210000051");
        String token = jwtUtils.generateJwtToken(member.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);

        mockMvc.perform(post("/api/access/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "totpCode": "000000"
                                }
                                """.formatted(member.getUserId())))
                .andExpect(status().isUnauthorized());

        assertTrue(hasLogFor(member.getUserId(), AccessResult.DENIED));
        assertFalse(hasLogFor(member.getUserId(), AccessResult.ALLOWED));
    }

    @Test
    @Transactional
    void validate_allowsActiveGuest() throws Exception {
        User creator = createMember("20210000052");
        Guest guest = createGuest("101011000", creator, GuestStatus.ACTIVE);
        String token = jwtUtils.generateJwtToken(creator.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);
        String code = currentCode(guest.getCodigo());

        mockMvc.perform(post("/api/access/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "totpCode": "%s"
                                }
                                """.formatted(guest.getGuestId(), code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));

        assertTrue(hasLogFor(guest.getGuestId(), AccessResult.ALLOWED));
    }

    @Test
    @Transactional
    void validate_deniesRevokedGuest() throws Exception {
        User creator = createMember("20210000053");
        Guest guest = createGuest("101011001", creator, GuestStatus.REVOKED);
        String token = jwtUtils.generateJwtToken(creator.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);

        mockMvc.perform(post("/api/access/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "totpCode": "000000"
                                }
                                """.formatted(guest.getGuestId())))
                .andExpect(status().isUnauthorized());

        assertTrue(hasLogFor(guest.getGuestId(), AccessResult.DENIED));
    }

    @Test
    @Transactional
    void validate_unknownSubjectIsLoggedAsDenied() throws Exception {
        User member = createMember("20210000054");
        String token = jwtUtils.generateJwtToken(member.getUserId(), tenant1.getTenantId(), Rol.MIEMBRO);

        mockMvc.perform(post("/api/access/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999999,
                                  "totpCode": "000000"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
