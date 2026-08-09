package org.glud.credentials.attendance.service;

import org.glud.credentials.attendance.dto.AttendanceResponseDTO;
import org.glud.credentials.event.dto.EventAttendanceResponseDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera archivos CSV separados por ";" (compatible con Excel es-CO),
 * con BOM UTF-8 para conservar acentos y encabezados.
 */
public final class AttendanceCsvExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String BOM = "\uFEFF";
    private static final String LINE_SEP = "\r\n";

    private AttendanceCsvExporter() {
    }

    public static String attendeesCsv(String eventTitle, List<EventAttendanceResponseDTO> attendees) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("Evento;").append(escape(eventTitle)).append(LINE_SEP)
          .append("Asistentes;").append(attendees.size()).append(LINE_SEP)
          .append("Código;Nombre;Fecha;Hora;Registrado por").append(LINE_SEP);
        for (EventAttendanceResponseDTO a : attendees) {
            sb.append(escape(a.codigo())).append(';')
              .append(escape(a.name())).append(';')
              .append(a.checkInAt().format(DATE)).append(';')
              .append(a.checkInAt().format(TIME)).append(';')
              .append(escape(a.markedByCodigo())).append(LINE_SEP);
        }
        return sb.toString();
    }

    public static String todayCsv(List<AttendanceResponseDTO> records) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("Código;Nombre;Rol;Grupo;Hora;Registrado por").append(LINE_SEP);
        for (AttendanceResponseDTO r : records) {
            sb.append(escape(r.codigo())).append(';')
              .append(escape(r.name())).append(';')
              .append(escape(r.rol().name())).append(';')
              .append(escape(r.tenantName())).append(';')
              .append(r.checkInAt().format(TIME)).append(';')
              .append(escape(r.markedByCodigo())).append(LINE_SEP);
        }
        return sb.toString();
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
