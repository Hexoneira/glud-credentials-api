package org.glud.credentials.security.components;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@NullMarked
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long userId = Long.parseLong(username);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return UserDetailsImpl.build(user);
    }
}
