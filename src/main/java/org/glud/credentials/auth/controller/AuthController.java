package org.glud.credentials.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Login endpoint",
            description = "Valida las credenciales del usuario y devuelve un token JWT si son válidas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "400", description = "Credenciales con formato inválido"),
            @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos")
    })
    public ResponseEntity<LoginResponseDTO> validateCredentials(@RequestBody @Valid LoginRequestDTO request){
        return new ResponseEntity<>(
                new LoginResponseDTO(userService.login(request)), HttpStatus.OK
        );
    }
}
