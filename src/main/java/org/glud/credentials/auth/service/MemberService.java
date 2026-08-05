package org.glud.credentials.auth.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.MemberCurrentResponseDTO;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.security.exception.MemberNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MemberCurrentResponseDTO getCurrentMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MemberNotFoundException(userId));
        return MemberCurrentResponseDTO.from(user);
    }
}
