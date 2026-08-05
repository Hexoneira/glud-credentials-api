package org.glud.credentials;

import org.glud.credentials.access.dto.AccessRequestDTO;
import org.glud.credentials.access.dto.AccessValidationResponseDTO;
import org.glud.credentials.access.event.AccessDecisionEvent;
import org.glud.credentials.access.model.AccessResult;
import org.glud.credentials.access.model.SubjectType;
import org.glud.credentials.access.service.AccessLogService;
import org.glud.credentials.access.service.AccessService;
import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.GuestRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.InvalidCredentialsException;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GuestRepository guestRepository;
    @Mock
    private TOTPService totpService;
    @Mock
    private AccessLogService accessLogService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccessService accessService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantId(1L);
        tenant.setName("GLUD");
        tenant.setTenantCode("GLUD");
        return tenant;
    }

    private User member(Long id, String codigo, UserStatus status) {
        User user = new User();
        user.setUserId(id);
        user.setCodigo(codigo);
        user.setStatus(status);
        user.setTenant(tenant());
        return user;
    }

    private Guest guest(Long id, String codigo, GuestStatus status) {
        Guest guest = new Guest();
        guest.setGuestId(id);
        guest.setCodigo(codigo);
        guest.setStatus(status);
        guest.setTenant(tenant());
        return guest;
    }

    private void authenticateAsTenantAdmin() {
        UserDetailsImpl principal = new UserDetailsImpl(
                1L, "20210000000", "x", 1L, Rol.TENANT_ADMIN, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private AccessRequestDTO memberRequest() {
        return new AccessRequestDTO("20210000001", SubjectType.MEMBER, "123456", "escanner-01", "Puerta principal");
    }

    private AccessRequestDTO guestRequest() {
        return new AccessRequestDTO("101011000", SubjectType.GUEST, "123456", null, null);
    }

    @Test
    void validate_allowsMemberWithValidTotp() throws Exception {
        when(userRepository.findByCodigo("20210000001")).thenReturn(Optional.of(member(1L, "20210000001", UserStatus.ACTIVE)));
        when(totpService.generateSeed("20210000001", "GLUD")).thenReturn("seed");
        when(totpService.verify("seed", "123456")).thenReturn(true);

        AccessValidationResponseDTO result = accessService.validate(memberRequest());

        assertTrue(result.allowed());
        assertEquals("Acceso permitido", result.message());
        assertNotNull(result.timestamp());
        verify(accessLogService).record(eq(SubjectType.MEMBER), eq(1L), eq("20210000001"), any(), eq("123456"),
                eq("escanner-01"), eq("Puerta principal"), eq(AccessResult.ALLOWED));
        verify(eventPublisher).publishEvent(any(AccessDecisionEvent.class));
    }

    @Test
    void validate_deniesWhenTotpInvalid() throws Exception {
        when(userRepository.findByCodigo("20210000001")).thenReturn(Optional.of(member(1L, "20210000001", UserStatus.ACTIVE)));
        when(totpService.generateSeed("20210000001", "GLUD")).thenReturn("seed");
        when(totpService.verify("seed", "123456")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> accessService.validate(memberRequest()));
        verify(accessLogService).record(eq(SubjectType.MEMBER), eq(1L), eq("20210000001"), any(), eq("123456"),
                eq("escanner-01"), eq("Puerta principal"), eq(AccessResult.DENIED));
        verify(eventPublisher).publishEvent(any(AccessDecisionEvent.class));
        verify(accessLogService, never()).record(any(), any(), any(), any(), anyString(), anyString(), anyString(),
                eq(AccessResult.ALLOWED));
    }

    @Test
    void validate_deniesWhenSubjectUnknown() throws Exception {
        when(userRepository.findByCodigo("999999")).thenReturn(Optional.empty());

        AccessRequestDTO req = new AccessRequestDTO("999999", SubjectType.MEMBER, "123456", null, null);

        assertThrows(InvalidCredentialsException.class, () -> accessService.validate(req));
        verify(accessLogService).record(eq(SubjectType.UNKNOWN), isNull(), eq("999999"), isNull(), eq("123456"),
                isNull(), isNull(), eq(AccessResult.DENIED));
    }

    @Test
    void validate_deniesWhenMemberSuspended() throws Exception {
        when(userRepository.findByCodigo("20210000001")).thenReturn(Optional.of(member(1L, "20210000001", UserStatus.SUSPENDED)));

        assertThrows(InvalidCredentialsException.class, () -> accessService.validate(memberRequest()));
    }

    @Test
    void validate_deniesWhenGuestNotActive() throws Exception {
        authenticateAsTenantAdmin();
        when(guestRepository.findByCodigoAndTenantTenantId("101011000", 1L))
                .thenReturn(Optional.of(guest(5L, "101011000", GuestStatus.REVOKED)));

        assertThrows(InvalidCredentialsException.class, () -> accessService.validate(guestRequest()));
    }

    @Test
    void validate_allowsGuestWithValidTotp() throws Exception {
        authenticateAsTenantAdmin();
        when(guestRepository.findByCodigoAndTenantTenantId("101011000", 1L))
                .thenReturn(Optional.of(guest(5L, "101011000", GuestStatus.ACTIVE)));
        when(totpService.generateSeed("101011000", "GLUD")).thenReturn("seed-guest");
        when(totpService.verify("seed-guest", "123456")).thenReturn(true);

        AccessValidationResponseDTO result = accessService.validate(guestRequest());

        assertTrue(result.allowed());
        verify(accessLogService).record(eq(SubjectType.GUEST), eq(5L), eq("101011000"), any(), eq("123456"),
                isNull(), isNull(), eq(AccessResult.ALLOWED));
        verify(userRepository, never()).findByCodigo(anyString());
    }

    @Test
    void validate_guestWithDefaultTypeIsNotResolvedAsMember() throws Exception {
        when(userRepository.findByCodigo("101011000")).thenReturn(Optional.empty());

        AccessRequestDTO req = new AccessRequestDTO("101011000", null, "123456", null, null);

        assertThrows(InvalidCredentialsException.class, () -> accessService.validate(req));
        verify(accessLogService).record(eq(SubjectType.UNKNOWN), isNull(), eq("101011000"), isNull(), eq("123456"),
                isNull(), isNull(), eq(AccessResult.DENIED));
    }
}
