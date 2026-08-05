package org.glud.credentials.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.CreateMemberRequestDTO;
import org.glud.credentials.auth.dto.MemberResponseDTO;
import org.glud.credentials.auth.dto.UpdateMemberRequestDTO;
import org.glud.credentials.auth.dto.UpdateMemberStatusRequestDTO;
import org.glud.credentials.auth.service.MemberAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberAdminController {

    private final MemberAdminService memberAdminService;

    @GetMapping
    @Operation(
            summary = "Lista los miembros del grupo",
            description = "Devuelve los miembros del grupo de trabajo al que pertenece el administrador autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de miembros obtenida correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de administrador de grupo")
    })
    public ResponseEntity<List<MemberResponseDTO>> findAll() {
        return new ResponseEntity<>(memberAdminService.findAll(), HttpStatus.OK);
    }

    @PostMapping
    @Operation(
            summary = "Crea un miembro",
            description = "Registra un nuevo miembro en el grupo de trabajo del administrador autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Miembro creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del miembro inválidos"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de administrador de grupo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un miembro con ese código")
    })
    public ResponseEntity<MemberResponseDTO> create(@RequestBody @Valid CreateMemberRequestDTO request) {
        return new ResponseEntity<>(memberAdminService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualiza un miembro",
            description = "Modifica el rol o email de un miembro del mismo grupo de trabajo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Miembro actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del miembro inválidos"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o miembro de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Miembro no encontrado")
    })
    public ResponseEntity<MemberResponseDTO> update(@PathVariable Long id,
                                                    @RequestBody @Valid UpdateMemberRequestDTO request) {
        return new ResponseEntity<>(memberAdminService.update(id, request), HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Cambia el estado de un miembro",
            description = "Suspende o reactiva las credenciales de un miembro del mismo grupo de trabajo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o miembro de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Miembro no encontrado"),
            @ApiResponse(responseCode = "409", description = "No puede modificar su propia cuenta")
    })
    public ResponseEntity<MemberResponseDTO> updateStatus(@PathVariable Long id,
                                                          @RequestBody @Valid UpdateMemberStatusRequestDTO request) {
        return new ResponseEntity<>(memberAdminService.updateStatus(id, request), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Elimina un miembro",
            description = "Elimina un miembro del mismo grupo de trabajo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Miembro eliminado correctamente"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o miembro de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Miembro no encontrado"),
            @ApiResponse(responseCode = "409", description = "No puede eliminar su propia cuenta")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberAdminService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
