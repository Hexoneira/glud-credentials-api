package org.glud.credentials.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.attendance.dto.AttendanceResponseDTO;
import org.glud.credentials.attendance.dto.RegisterAttendanceRequestDTO;
import org.glud.credentials.attendance.service.AttendanceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final AttendanceService attendanceService;

    @PostMapping("/register")
    @Operation(
            summary = "Registra asistencia de un miembro",
            description = "Registra la asistencia del día escaneando el código QR del carnet o escribiendo el código del miembro"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Asistencia registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "El código escaneado no es válido"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes o miembro de otro grupo"),
            @ApiResponse(responseCode = "404", description = "Miembro no encontrado"),
            @ApiResponse(responseCode = "409", description = "El miembro ya registró asistencia hoy o está suspendido")
    })
    public ResponseEntity<AttendanceResponseDTO> register(
            @RequestBody @Valid RegisterAttendanceRequestDTO request) {
        return new ResponseEntity<>(attendanceService.registerAttendance(request), HttpStatus.CREATED);
    }

    @GetMapping("/today")
    @Operation(
            summary = "Lista la asistencia de hoy",
            description = "Devuelve los registros de asistencia del día. El admin de grupo solo ve su propio tenant"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registros obtenidos correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de administrador")
    })
    public ResponseEntity<List<AttendanceResponseDTO>> today() {
        return new ResponseEntity<>(attendanceService.todayAttendance(), HttpStatus.OK);
    }

    @GetMapping(value = "/today/export", produces = "text/csv")
    @Operation(
            summary = "Exporta la asistencia de hoy en CSV",
            description = "Descarga un CSV de los registros del día, compatible con Excel (separador \";\" y BOM UTF-8)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV generado correctamente"),
            @ApiResponse(responseCode = "403", description = "Se requieren permisos de administrador")
    })
    public ResponseEntity<String> exportToday() {
        String csv = attendanceService.exportTodayCsv();
        String filename = "asistencia-" + LocalDate.now(COLOMBIA) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
