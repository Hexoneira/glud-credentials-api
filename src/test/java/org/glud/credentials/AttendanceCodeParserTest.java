package org.glud.credentials;

import org.glud.credentials.attendance.service.AttendanceCodeParser;
import org.glud.credentials.security.exception.InvalidScannedCodeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceCodeParserTest {

    @Test
    void extractCodigo_acceptsPlainCodigo() {
        assertEquals("20210000001", AttendanceCodeParser.extractCodigo("20210000001"));
    }

    @Test
    void extractCodigo_acceptsGuestCodigo() {
        assertEquals("guest-e2e", AttendanceCodeParser.extractCodigo("guest-e2e"));
    }

    @Test
    void extractCodigo_extractsFromCarnetQrPayload() {
        assertEquals("20210000001", AttendanceCodeParser.extractCodigo("ID:20210000001|TOTP:123456"));
    }

    @Test
    void extractCodigo_trimsWhitespace() {
        assertEquals("20210000001", AttendanceCodeParser.extractCodigo("  20210000001  "));
    }

    @Test
    void extractCodigo_throws_whenNull() {
        assertThrows(InvalidScannedCodeException.class, () -> AttendanceCodeParser.extractCodigo(null));
    }

    @Test
    void extractCodigo_throws_whenEmpty() {
        assertThrows(InvalidScannedCodeException.class, () -> AttendanceCodeParser.extractCodigo(""));
    }

    @Test
    void extractCodigo_throws_whenUnrecognizedPayload() {
        assertThrows(InvalidScannedCodeException.class, () -> AttendanceCodeParser.extractCodigo("https://example.com/xyz"));
    }

    @Test
    void extractCodigo_throws_whenIdWithoutTotp() {
        assertThrows(InvalidScannedCodeException.class, () -> AttendanceCodeParser.extractCodigo("ID:20210000001"));
    }

    @Test
    void extractCodigo_throws_whenTotpNotSixDigits() {
        assertThrows(InvalidScannedCodeException.class, () -> AttendanceCodeParser.extractCodigo("ID:20210000001|TOTP:12"));
        assertThrows(InvalidScannedCodeException.class, () -> AttendanceCodeParser.extractCodigo("ID:20210000001|TOTP:1234567"));
    }

    @Test
    void extractCodigo_throws_whenTooShort() {
        assertThrows(InvalidScannedCodeException.class, () -> AttendanceCodeParser.extractCodigo("ab"));
    }

    @Test
    void extractTotp_returnsCode_fromQrPayload() {
        assertEquals("123456", AttendanceCodeParser.extractTotp("ID:20210000001|TOTP:123456"));
    }

    @Test
    void extractTotp_returnsNull_whenPlainCodigo() {
        assertNull(AttendanceCodeParser.extractTotp("20210000001"));
    }

    @Test
    void extractTotp_returnsNull_whenNull() {
        assertNull(AttendanceCodeParser.extractTotp(null));
    }

    @Test
    void extractTotp_returnsNull_whenUnrecognizedPayload() {
        assertNull(AttendanceCodeParser.extractTotp("https://example.com/xyz"));
    }
}
