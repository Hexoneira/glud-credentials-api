package org.glud.credentials;

import org.glud.credentials.auth.dto.LoginRequestDTO;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.auth.service.UserService;
import org.glud.credentials.security.components.JwtUtils;
import org.glud.credentials.security.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private User user;
    @Mock
    private Tenant tenant;

    @InjectMocks
    private UserService userService;

    @Test
    void login_returnsToken_whenCredentialsAreValid() {
        LoginRequestDTO request = new LoginRequestDTO("validUser", "validPassword");

        when(userRepository.findByUsername("validUser")).thenReturn(Optional.of(user));
        when(user.getPassword()).thenReturn("hashedPassword");
        when(passwordEncoder.matches("validPassword", "hashedPassword")).thenReturn(true);
        when(user.getUserId()).thenReturn(1L);
        when(user.getTenant()).thenReturn(tenant);
        when(tenant.getTenantId()).thenReturn(10L);
        when(user.getRol()).thenReturn(Rol.MIEMBRO);
        when(jwtUtils.generateJwtToken(1L, 10L, Rol.MIEMBRO)).thenReturn("mockedJwtToken");

        String token = userService.login(request);

        assertEquals("mockedJwtToken", token);
        verify(jwtUtils).generateJwtToken(1L, 10L, Rol.MIEMBRO);
    }

    @Test
    void login_throwsInvalidCredentials_whenUserNotFound() {
        LoginRequestDTO request = new LoginRequestDTO("unknownUser", "anyPassword");
        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordDoesNotMatch() {
        LoginRequestDTO request = new LoginRequestDTO("validUser", "wrongPassword");

        when(userRepository.findByUsername("validUser")).thenReturn(Optional.of(user));
        when(user.getPassword()).thenReturn("hashedPassword");
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
        verifyNoInteractions(jwtUtils);
    }
}
