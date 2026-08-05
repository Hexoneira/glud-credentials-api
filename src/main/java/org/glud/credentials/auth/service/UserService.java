package org.glud.credentials.auth.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.model.UserStatus;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.auth.dto.LoginRequestDTO;
import org.glud.credentials.security.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.glud.credentials.security.components.JwtUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public String login(LoginRequestDTO loginRequest) {
        User existingUser = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos"));

        if (!passwordEncoder.matches(loginRequest.password(), existingUser.getPassword())){
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }
        if (existingUser.getStatus() == UserStatus.SUSPENDED) {
            throw new InvalidCredentialsException("Credencial suspendida");
        }
        return jwtUtils.generateJwtToken(
                existingUser.getUserId(), existingUser.getTenant().getTenantId(), existingUser.getRol()
        );
    }
}

