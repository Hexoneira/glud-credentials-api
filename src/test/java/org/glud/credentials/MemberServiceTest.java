package org.glud.credentials;

import org.glud.credentials.auth.dto.MemberCurrentResponseDTO;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.auth.service.MemberService;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final String SEED = "4ljjwnrzlorsnlhdmitrl4rubdftyhc64bt3qqsnhjbdbq2uqyhq";

    @Mock
    private UserRepository userRepository;

    @Mock
    private TOTPService totpService;

    @InjectMocks
    private MemberService memberService;

    private User member() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(1L);
        tenant.setName("GLUD");
        tenant.setTenantCode("GLUD");

        User user = new User();
        user.setUserId(5L);
        user.setUsername("20210000000");
        user.setPassword("hashed");
        user.setCodigo("20210000000");
        user.setEmail("miembro@udistrital.edu.co");
        user.setTenant(tenant);
        user.setRol(Rol.MIEMBRO);
        return user;
    }

    @Test
    void getCurrentMember_returnsMappedCredentialWithSeed() throws Exception {
        when(userRepository.findById(5L)).thenReturn(Optional.of(member()));
        when(totpService.generateSeed("20210000000", "GLUD")).thenReturn(SEED);

        MemberCurrentResponseDTO result = memberService.getCurrentMember(5L);

        assertEquals("20210000000", result.id());
        assertEquals("20210000000", result.name());
        assertEquals("miembro@udistrital.edu.co", result.email());
        assertEquals("MIEMBRO", result.role());
        assertEquals(List.of("GLUD"), result.groups());
        assertNull(result.icon());
        assertEquals(SEED, result.totpSecret());
    }

    @Test
    void getCurrentMember_throwsNotFound_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class, () -> memberService.getCurrentMember(99L));
    }
}
