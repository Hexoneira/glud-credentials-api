package org.glud.credentials.totp_seed.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.auth.service.MemberService;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
public class CredentialsController {

    private final TOTPService totpService;
    private final MemberService memberService;

    @GetMapping("/seed")
    @Operation(
            summary = "Obtiene la semilla TOTP del miembro autenticado",
            description = "Devuelve la semilla determinista del carnet para generar el código dinámico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Semilla generada correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Miembro no encontrado")
    })
    public ResponseEntity<String> getSeed() throws NoSuchAlgorithmException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        User user = memberService.loadUser(principal.getUserId());
        return new ResponseEntity<>(
                totpService.generateSeed(user.getCodigo(), user.getTenant().getTenantCode()),
                HttpStatus.OK
        );
    }
}
