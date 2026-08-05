package org.glud.credentials;

import org.glud.credentials.auth.dto.CreateGuestRequestDTO;
import org.glud.credentials.auth.dto.GuestResponseDTO;
import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.GuestRepository;
import org.glud.credentials.auth.repository.TenantRepository;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.auth.service.GuestService;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.ActiveGuestAlreadyExistsException;
import org.glud.credentials.security.exception.GuestLimitExceededException;
import org.glud.credentials.security.exception.GuestLinkExpiredException;
import org.glud.credentials.security.exception.GuestNotFoundException;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.glud.credentials.security.exception.TenantNotFoundException;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock
    private GuestRepository guestRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TOTPService totpService;

    private GuestService guestService;

    private final RoleGuard roleGuard = new RoleGuard();

    @BeforeEach
    void setUp() {
        guestService = new GuestService(guestRepository, tenantRepository, userRepository, roleGuard, totpService);
        ReflectionTestUtils.setField(guestService, "maxActiveGuests", 15);
        ReflectionTestUtils.setField(guestService, "accessDuration", Duration.ofHours(2));
        ReflectionTestUtils.setField(guestService, "retention", Duration.ofDays(21));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, Long tenantId, Rol rol) {
        UserDetailsImpl principal = new UserDetailsImpl(
                userId, "usuario", "pw", tenantId, rol,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Tenant tenant(Long id) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(id);
        tenant.setName("GLUD");
        tenant.setTenantCode("GLUD");
        return tenant;
    }

    private User member(Long id) {
        User user = new User();
        user.setUserId(id);
        user.setCodigo("20210000001");
        return user;
    }

    @Test
    void create_createsGuestInTenantOfCreator() {
        authenticate(10L, 1L, Rol.MIEMBRO);
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant(1L)));
        when(guestRepository.existsByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(false);
        when(guestRepository.countByTenantTenantIdAndStatus(1L, GuestStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.getReferenceById(10L)).thenReturn(member(10L));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> {
            Guest saved = invocation.getArgument(0);
            saved.setGuestId(5L);
            return saved;
        });

        CreateGuestRequestDTO request = new CreateGuestRequestDTO("101011000", "Invitada Uno", "inv1@mail.com");

        GuestResponseDTO result = guestService.create(request);

        assertEquals(5L, result.id());
        assertEquals("101011000", result.codigo());
        assertEquals("Invitada Uno", result.name());
        assertEquals("inv1@mail.com", result.email());
        assertEquals(GuestStatus.ACTIVE, result.status());
        assertEquals(1L, result.tenantId());
        assertEquals(10L, result.createdById());
        verify(tenantRepository).findByIdForUpdate(1L);
        verify(guestRepository).save(argThat(g ->
                g.getTenant().getTenantId().equals(1L) && g.getCreatedBy().getUserId().equals(10L)));
    }

    @Test
    void create_throwsActiveGuestAlreadyExists_whenCreatorHasActiveGuest() {
        authenticate(10L, 1L, Rol.MIEMBRO);
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant(1L)));
        when(guestRepository.existsByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(true);

        CreateGuestRequestDTO request = new CreateGuestRequestDTO("101011000", "Invitada Uno", null);

        assertThrows(ActiveGuestAlreadyExistsException.class, () -> guestService.create(request));
        verify(guestRepository, never()).save(any());
    }

    @Test
    void create_throwsLimitExceeded_whenTenantAtLimit() {
        authenticate(10L, 1L, Rol.MIEMBRO);
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant(1L)));
        when(guestRepository.existsByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(false);
        when(guestRepository.countByTenantTenantIdAndStatus(1L, GuestStatus.ACTIVE)).thenReturn(15L);

        CreateGuestRequestDTO request = new CreateGuestRequestDTO("101011000", "Invitada Uno", null);

        assertThrows(GuestLimitExceededException.class, () -> guestService.create(request));
        verify(guestRepository, never()).save(any());
    }

    @Test
    void create_allowsCreationWhenTenantBelowLimit() {
        authenticate(10L, 1L, Rol.MIEMBRO);
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant(1L)));
        when(guestRepository.existsByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(false);
        when(guestRepository.countByTenantTenantIdAndStatus(1L, GuestStatus.ACTIVE)).thenReturn(14L);
        when(userRepository.getReferenceById(10L)).thenReturn(member(10L));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateGuestRequestDTO request = new CreateGuestRequestDTO("101011000", "Invitada Uno", null);

        GuestResponseDTO result = guestService.create(request);

        assertEquals(GuestStatus.ACTIVE, result.status());
        verify(guestRepository).save(any(Guest.class));
    }

    @Test
    void create_throwsRoleRequired_whenInvitado() {
        authenticate(10L, 1L, Rol.INVITADO);

        CreateGuestRequestDTO request = new CreateGuestRequestDTO("101011000", "Invitada Uno", null);

        assertThrows(RoleRequiredException.class, () -> guestService.create(request));
        verify(tenantRepository, never()).findByIdForUpdate(any());
        verify(guestRepository, never()).save(any());
    }

    @Test
    void create_throwsTenantNotFound_whenTenantMissing() {
        authenticate(10L, 99L, Rol.MIEMBRO);
        when(tenantRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        CreateGuestRequestDTO request = new CreateGuestRequestDTO("101011000", "Invitada Uno", null);

        assertThrows(TenantNotFoundException.class, () -> guestService.create(request));
    }

    @Test
    void getCurrentGuest_returnsActiveGuestWithDerivedSeed() throws Exception {
        authenticate(10L, 1L, Rol.MIEMBRO);
        Guest guest = new Guest();
        guest.setGuestId(5L);
        guest.setCodigo("101011000");
        guest.setName("Invitada Uno");
        guest.setTenant(tenant(1L));
        guest.setStatus(GuestStatus.ACTIVE);
        guest.setCreatedBy(member(10L));
        when(guestRepository.findByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(Optional.of(guest));
        when(totpService.generateSeed("101011000", "GLUD")).thenReturn("seed-guest");

        GuestResponseDTO result = guestService.getCurrentGuest();

        assertEquals(5L, result.id());
        assertEquals(GuestStatus.ACTIVE, result.status());
        assertEquals("seed-guest", result.totpSecret());
    }

    @Test
    void getCurrentGuest_throwsNotFound_whenNoActiveGuest() {
        authenticate(10L, 1L, Rol.MIEMBRO);
        when(guestRepository.findByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(GuestNotFoundException.class, () -> guestService.getCurrentGuest());
    }

    @Test
    void create_setsAccessTokenAndTwoHourExpiry() {
        authenticate(10L, 1L, Rol.MIEMBRO);
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant(1L)));
        when(guestRepository.existsByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(false);
        when(guestRepository.countByTenantTenantIdAndStatus(1L, GuestStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.getReferenceById(10L)).thenReturn(member(10L));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> {
            Guest saved = invocation.getArgument(0);
            saved.setGuestId(5L);
            return saved;
        });

        LocalDateTime before = LocalDateTime.now();
        GuestResponseDTO result = guestService.create(new CreateGuestRequestDTO("101011000", "Invitada Uno", null));
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result.accessToken());
        assertEquals(48, result.accessToken().length());
        assertNotNull(result.expiresAt());
        assertTrue(!result.expiresAt().isBefore(before.plusHours(2).minusSeconds(1)));
        assertTrue(!result.expiresAt().isAfter(after.plusHours(2).plusSeconds(1)));
    }

    @Test
    void create_expiresStaleGuestBeforeCheckingActive() {
        authenticate(10L, 1L, Rol.MIEMBRO);
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant(1L)));

        Guest stale = new Guest();
        stale.setGuestId(3L);
        stale.setStatus(GuestStatus.ACTIVE);
        stale.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(guestRepository.findByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(Optional.of(stale));
        when(guestRepository.existsByCreatedByUserIdAndStatus(10L, GuestStatus.ACTIVE)).thenReturn(false);
        when(guestRepository.countByTenantTenantIdAndStatus(1L, GuestStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.getReferenceById(10L)).thenReturn(member(10L));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        guestService.create(new CreateGuestRequestDTO("101011000", "Invitada Uno", null));

        assertEquals(GuestStatus.EXPIRED, stale.getStatus());
    }

    @Test
    void accessGuest_returnsGuestWithSeed_whenLinkValid() throws Exception {
        Guest guest = new Guest();
        guest.setGuestId(5L);
        guest.setCodigo("101011000");
        guest.setName("Invitada Uno");
        guest.setStatus(GuestStatus.ACTIVE);
        guest.setExpiresAt(LocalDateTime.now().plusHours(1));
        guest.setTenant(tenant(1L));
        guest.setCreatedBy(member(10L));
        when(guestRepository.findByAccessToken("tok")).thenReturn(Optional.of(guest));
        when(totpService.generateSeed("101011000", "GLUD")).thenReturn("seed-guest");

        GuestResponseDTO result = guestService.accessGuest("tok");

        assertEquals(5L, result.id());
        assertEquals("seed-guest", result.totpSecret());
        assertEquals(GuestStatus.ACTIVE, result.status());
    }

    @Test
    void accessGuest_throwsExpired_whenLinkExpired() {
        Guest guest = new Guest();
        guest.setGuestId(5L);
        guest.setStatus(GuestStatus.ACTIVE);
        guest.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(guestRepository.findByAccessToken("tok")).thenReturn(Optional.of(guest));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(GuestLinkExpiredException.class, () -> guestService.accessGuest("tok"));
        assertEquals(GuestStatus.EXPIRED, guest.getStatus());
    }

    @Test
    void accessGuest_throwsExpired_whenAlreadyExpiredStatus() {
        Guest guest = new Guest();
        guest.setGuestId(5L);
        guest.setStatus(GuestStatus.EXPIRED);
        guest.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(guestRepository.findByAccessToken("tok")).thenReturn(Optional.of(guest));

        assertThrows(GuestLinkExpiredException.class, () -> guestService.accessGuest("tok"));
    }

    @Test
    void accessGuest_throwsNotFound_whenUnknownToken() {
        when(guestRepository.findByAccessToken("unknown")).thenReturn(Optional.empty());

        assertThrows(GuestNotFoundException.class, () -> guestService.accessGuest("unknown"));
    }

    @Test
    void listMyGuests_returnsAllGuestsNewestFirst() throws Exception {
        authenticate(10L, 1L, Rol.MIEMBRO);
        Guest active = new Guest();
        active.setGuestId(1L);
        active.setCodigo("101011000");
        active.setStatus(GuestStatus.ACTIVE);
        active.setTenant(tenant(1L));
        active.setCreatedBy(member(10L));
        Guest expired = new Guest();
        expired.setGuestId(2L);
        expired.setStatus(GuestStatus.EXPIRED);
        expired.setTenant(tenant(1L));
        expired.setCreatedBy(member(10L));
        when(guestRepository.findAllByCreatedByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(active, expired));
        when(totpService.generateSeed("101011000", "GLUD")).thenReturn("seed-active");

        List<GuestResponseDTO> result = guestService.listMyGuests();

        assertEquals(2, result.size());
        assertEquals(GuestStatus.ACTIVE, result.get(0).status());
        assertEquals("seed-active", result.get(0).totpSecret());
        assertEquals(GuestStatus.EXPIRED, result.get(1).status());
        assertNull(result.get(1).totpSecret());
    }

    @Test
    void purgeExpiredGuests_deletesGuestsOlderThanRetention() {
        guestService.purgeExpiredGuests();
        LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofDays(21));
        verify(guestRepository).deleteByCreatedAtBefore(argThat(limit -> limit.isAfter(cutoff.minusSeconds(1)) && limit.isBefore(cutoff.plusSeconds(1))));
    }
}
