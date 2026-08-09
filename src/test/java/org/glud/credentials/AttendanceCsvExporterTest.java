package org.glud.credentials;

import org.glud.credentials.attendance.dto.AttendanceResponseDTO;
import org.glud.credentials.attendance.service.AttendanceCsvExporter;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.event.dto.EventAttendanceResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceCsvExporterTest {

    private static final LocalDateTime CHECK_IN = LocalDateTime.of(2026, 8, 5, 18, 5, 30);

    @Test
    void attendeesCsv_includesBomHeaderAndRows() {
        String csv = AttendanceCsvExporter.attendeesCsv("Asamblea \"GLUD\" 2026", List.of(
                new EventAttendanceResponseDTO(1L, 2L, "20210000002", "María, Gómez", CHECK_IN, "20219999999")));

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Evento;"));
        assertTrue(csv.contains("\"Asamblea \"\"GLUD\"\" 2026\""));
        assertTrue(csv.contains("Asistentes;1"));
        assertTrue(csv.contains("Código;Nombre;Fecha;Hora;Registrado por"));
        assertTrue(csv.contains("\"20210000002\";\"María, Gómez\";05/08/2026;18:05:30;\"20219999999\""));
    }

    @Test
    void attendeesCsv_escapesNullFields() {
        String csv = AttendanceCsvExporter.attendeesCsv("Asamblea", List.of(
                new EventAttendanceResponseDTO(1L, 2L, "20210000002", null, CHECK_IN, null)));

        assertTrue(csv.contains("\"20210000002\";\"\";"));
        assertTrue(csv.endsWith("\"\"\r\n"));
    }

    @Test
    void attendeesCsv_emptyListStillHasHeaders() {
        String csv = AttendanceCsvExporter.attendeesCsv("Asamblea", List.of());

        assertTrue(csv.contains("Asistentes;0"));
        assertTrue(csv.contains("Código;Nombre;Fecha;Hora;Registrado por"));
    }

    @Test
    void todayCsv_includesBomHeaderAndRows() {
        String csv = AttendanceCsvExporter.todayCsv(List.of(
                new AttendanceResponseDTO(1L, "20210000002", "María Gómez", "m2@glud.org",
                        Rol.MIEMBRO, 1L, "GLUD", CHECK_IN, "20219999999")));

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Código;Nombre;Rol;Grupo;Hora;Registrado por"));
        assertTrue(csv.contains("\"20210000002\";\"María Gómez\";\"MIEMBRO\";\"GLUD\";18:05:30;\"20219999999\""));
    }

    @Test
    void todayCsv_emptyListStillHasHeaders() {
        String csv = AttendanceCsvExporter.todayCsv(List.of());

        assertTrue(csv.contains("Código;Nombre;Rol;Grupo;Hora;Registrado por"));
    }
}
