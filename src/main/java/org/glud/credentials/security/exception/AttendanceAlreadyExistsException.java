package org.glud.credentials.security.exception;

public class AttendanceAlreadyExistsException extends RuntimeException {

    public AttendanceAlreadyExistsException(String codigo) {
        super("El miembro " + codigo + " ya registró su asistencia hoy");
    }
}
