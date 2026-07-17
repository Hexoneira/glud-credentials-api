package org.glud.credentials.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.service.UserService;
import org.glud.credentials.auth.dto.LoginRequestDTO;
import org.glud.credentials.auth.dto.LoginResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/api/auth/login")
    public ResponseEntity<LoginResponseDTO> validateCredentials(@RequestBody @Valid LoginRequestDTO request){
        return new ResponseEntity<>(
                new LoginResponseDTO(userService.login(request)), HttpStatus.OK
        );
    }
}
