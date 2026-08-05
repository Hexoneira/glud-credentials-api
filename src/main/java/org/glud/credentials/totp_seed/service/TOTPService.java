package org.glud.credentials.totp_seed.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
public class TOTPService {

    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    @Value("${totp.secret}")
    private String serverSecret;

    /**
     * Deriva la semilla TOTP determinista de un miembro: SHA-256 de
     * {@code codigo:tenantCode:serverSecret}, codificado en Base32 (RFC 4648)
     * para ser compatible con otplib / Google Authenticator.
     */
    public String generateSeed(String codigo, String tenantCode) throws NoSuchAlgorithmException {
        String source = codigo + ":" + tenantCode + ":" + serverSecret;
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
        return base32Encode(hash).toLowerCase(Locale.ROOT);
    }

    static String base32Encode(byte[] data) {
        StringBuilder encoded = new StringBuilder();
        int bits = 0;
        int buffer = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                encoded.append(BASE32_ALPHABET[(buffer >> (bits - 5)) & 0x1F]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            encoded.append(BASE32_ALPHABET[(buffer << (5 - bits)) & 0x1F]);
        }
        return encoded.toString();
    }
}
