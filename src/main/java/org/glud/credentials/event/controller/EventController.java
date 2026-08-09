package org.glud.credentials.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.event.dto.CreateEventRequestDTO;
import org.glud.credentials.event.dto.EventAttendanceResponseDTO;
import org.glud.credentials.event.dto.EventResponseDTO;
import org.glud.credentials.event.service.EventService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(
            summary = "Lista los eventos",
            description = "Devuelve los eventos con su estado y número de asistentes. El admin de grupo solo ve su tenant; el super admin ve todos"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Eventos obtenidos correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de administrador")
    })
    public ResponseEntity<List<EventResponseDTO>> findAll() {
        return new ResponseEntity<>(eventService.findAll(), HttpStatus.OK);
    }

    @PostMapping
    @Operation(
            summary = "Crea un evento o reunión",
            description = "Registra un evento con título y hora de inicio. El super admin debe indicar el grupo (tenantId)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del evento inválidos"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de administrador")
    })
    public ResponseEntity<EventResponseDTO> create(@RequestBody @Valid CreateEventRequestDTO request) {
        return new ResponseEntity<>(eventService.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de un evento",
            description = "Devuelve el evento con su estado y el número de asistentes"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento obtenido correctamente"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o evento de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    public ResponseEntity<EventResponseDTO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(eventService.findById(id), HttpStatus.OK);
    }

    @GetMapping("/{id}/attendance")
    @Operation(
            summary = "Lista los asistentes de un evento",
            description = "Devuelve los miembros que registraron asistencia escaneando su carnet en el evento"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asistentes obtenidos correctamente"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o evento de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    public ResponseEntity<List<EventAttendanceResponseDTO>> attendees(@PathVariable Long id) {
        return new ResponseEntity<>(eventService.attendees(id), HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/attendance/export", produces = "text/csv")
    @Operation(
            summary = "Exporta la lista de asistentes de un evento en CSV",
            description = "Descarga un CSV de los asistentes del evento, compatible con Excel (separador \";\" y BOM UTF-8)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV generado correctamente"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o evento de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    public ResponseEntity<String> exportAttendance(@PathVariable Long id) {
        String csv = eventService.exportAttendeesCsv(id);
        String filename = "asistentes-evento-" + id + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Elimina un evento",
            description = "Elimina un evento sin asistencia registrada"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Evento eliminado correctamente"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o evento de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "El evento tiene asistencia registrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
