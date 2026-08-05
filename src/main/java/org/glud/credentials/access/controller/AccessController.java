package org.glud.credentials.access.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.access.dto.AccessRequestDTO;
import org.glud.credentials.access.dto.AccessValidationResponseDTO;
import org.glud.credentials.access.service.AccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessController {

    private final AccessService accessService;

    @PostMapping("/validate")
    @Operation(
            summary = "Valida acceso por TOTP",
            description = "Recupera la seed del sujeto (miembro o invitado), calcula el TOTP actual y lo compara " +
                    "con el recibido. Registra cada intento en AccessLogs (auditoría) y emite un evento " +
                    "para la notificación de la cerradura."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Acceso permitido"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "401", description = "Código inválido, expirado o credencial desconocida"),
            @ApiResponse(responseCode = "500", description = "Error interno al generar el TOTP")
    })
    public ResponseEntity<AccessValidationResponseDTO> validate(
            @RequestBody @Valid AccessRequestDTO request) throws NoSuchAlgorithmException, InvalidKeyException {
        return new ResponseEntity<>(accessService.validate(request), HttpStatus.OK);
    }
}
