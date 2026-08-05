package org.glud.credentials.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.MemberCurrentResponseDTO;
import org.glud.credentials.auth.service.MemberService;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/current")
    @Operation(
            summary = "Obtiene la credencial del miembro autenticado",
            description = "Devuelve los datos del miembro actual para poblar su carnet digital"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credencial obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Miembro no encontrado")
    })
    public ResponseEntity<MemberCurrentResponseDTO> getCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(memberService.getCurrentMember(principal.getUserId()), HttpStatus.OK);
    }
}
