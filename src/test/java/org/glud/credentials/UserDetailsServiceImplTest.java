package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.components.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_returnsUserDetails_whenUserExists() {
        User user = mock(User.class);
        Tenant tenant = mock(Tenant.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getTenant()).thenReturn(tenant);
        when(tenant.getTenantId()).thenReturn(10L);
        when(user.getRol()).thenReturn(Rol.MIEMBRO);

        UserDetails result = userDetailsService.loadUserByUsername("1");

        assertNotNull(result);
    }

    @Test
    void loadUserByUsername_throwsException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("99"));
    }
}