package org.glud.credentials.attendance.service;

import org.glud.credentials.security.exception.InvalidScannedCodeException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae el código de miembro a partir del contenido escaneado.
 * Soporta dos formatos:
 *  <ul>
 *      <li>Un código plano (ej. "20210000001")</li>
 *      <li>El payload QR del carnet (ej. "ID:20210000001|TOTP:123456")</li>
 *  </ul>
 */
public final class AttendanceCodeParser {

    private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9_-]{3,30}");
    private static final Pattern ID_PREFIX = Pattern.compile("^ID:([A-Za-z0-9_-]{3,30})\\|TOTP:");

    private AttendanceCodeParser() {
    }

    public static String extractCodigo(String scanned) {
        String value = scanned == null ? "" : scanned.trim();
        if (CODIGO.matcher(value).matches()) {
            return value;
        }
        Matcher matcher = ID_PREFIX.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new InvalidScannedCodeException();
    }
}
