package org.glud.credentials.auth.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.MemberCurrentResponseDTO;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final TOTPService totpService;

    @Transactional(readOnly = true)
    public MemberCurrentResponseDTO getCurrentMember(Long userId) {
        User user = loadUser(userId);
        return MemberCurrentResponseDTO.from(user, generateSeed(user));
    }

    @Transactional(readOnly = true)
    public User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MemberNotFoundException(userId));
    }

    private String generateSeed(User user) {
        try {
            return totpService.generateSeed(user.getCodigo(), user.getTenant().getTenantCode());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
