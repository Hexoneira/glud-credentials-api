package org.glud.credentials.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.CreateGuestRequestDTO;
import org.glud.credentials.auth.dto.GuestResponseDTO;
import org.glud.credentials.auth.service.GuestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @PostMapping
    @Operation(
            summary = "Registra un invitado temporal",
            description = "Un miembro registra a un invitado temporal en su grupo de trabajo. " +
                    "Valida que el miembro no tenga otro invitado activo (RF-2.1) y que el grupo " +
                    "no supere el límite de invitados activos (RF-2.2)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invitado registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del invitado inválidos"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o límite de invitados alcanzado"),
            @ApiResponse(responseCode = "409", description = "El miembro ya tiene un invitado activo")
    })
    public ResponseEntity<GuestResponseDTO> create(@RequestBody @Valid CreateGuestRequestDTO request) {
        return new ResponseEntity<>(guestService.create(request), HttpStatus.CREATED);
    }
}
