package org.glud.credentials.security.exception;

public class AttendanceAlreadyExistsException extends RuntimeException {

    public AttendanceAlreadyExistsException(String codigo) {
        super("El miembro " + codigo + " ya registró su asistencia hoy");
    }

    public AttendanceAlreadyExistsException(String codigo, String eventTitle) {
        super("El miembro " + codigo + " ya registró asistencia a \"" + eventTitle + "\"");
    }
}
