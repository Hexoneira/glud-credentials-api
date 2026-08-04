package org.glud.credentials.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.dto.CreateTenantRequestDTO;
import org.glud.credentials.auth.dto.TenantResponseDTO;
import org.glud.credentials.auth.dto.UpdateTenantRequestDTO;
import org.glud.credentials.auth.service.TenantService;
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
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @Operation(
            summary = "Lista los grupos de trabajo",
            description = "Devuelve todos los grupos de trabajo con su número de miembros actual"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de grupos obtenida correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de super administrador")
    })
    public ResponseEntity<List<TenantResponseDTO>> findAll() {
        return new ResponseEntity<>(tenantService.findAll(), HttpStatus.OK);
    }

    @PostMapping
    @Operation(
            summary = "Crea un grupo de trabajo",
            description = "Registra un nuevo grupo de trabajo en el sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Grupo creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del grupo inválidos"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de super administrador"),
            @ApiResponse(responseCode = "409", description = "Ya existe un grupo con ese código")
    })
    public ResponseEntity<TenantResponseDTO> create(@RequestBody @Valid CreateTenantRequestDTO request) {
        return new ResponseEntity<>(tenantService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualiza un grupo de trabajo",
            description = "Modifica el nombre, director o límite de miembros de un grupo existente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupo actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del grupo inválidos"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de super administrador"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<TenantResponseDTO> update(@PathVariable Long id,
                                                    @RequestBody @Valid UpdateTenantRequestDTO request) {
        return new ResponseEntity<>(tenantService.update(id, request), HttpStatus.OK);
    }

    @PatchMapping("/{id}/suspend")
    @Operation(
            summary = "Suspende un grupo de trabajo",
            description = "Cambia el estado del grupo a SUSPENDED"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupo suspendido correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de super administrador"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<TenantResponseDTO> suspend(@PathVariable Long id) {
        return new ResponseEntity<>(tenantService.suspend(id), HttpStatus.OK);
    }

    @PatchMapping("/{id}/reactivate")
    @Operation(
            summary = "Reactiva un grupo de trabajo",
            description = "Cambia el estado del grupo a ACTIVE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupo reactivado correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de super administrador"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<TenantResponseDTO> reactivate(@PathVariable Long id) {
        return new ResponseEntity<>(tenantService.reactivate(id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Elimina un grupo de trabajo",
            description = "Elimina un grupo de trabajo sin miembros asociados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Grupo eliminado correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de super administrador"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El grupo aún tiene miembros asociados")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
