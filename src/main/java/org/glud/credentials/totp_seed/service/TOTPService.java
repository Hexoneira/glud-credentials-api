package org.glud.credentials.totp_seed.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;

@Service
public class TOTPService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;

    @Value("${totp.secret}")
    private String serverSecret;

    @Value("${totp.window:1}")
    private int clockDriftWindow;

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

    /**
     * Verifica un código TOTP de 6 dígitos contra la semilla, aceptando un
     * desfase de {@code clockDriftWindow} pasos de 30 segundos (RFC 6238).
     */
    public boolean verify(String base32Seed, String code) throws NoSuchAlgorithmException, InvalidKeyException {
        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (long offset = -clockDriftWindow; offset <= clockDriftWindow; offset++) {
            if (generateCode(base32Seed, timeIndex + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    // RFC 6238 (TOTP) defines HMAC-SHA1 as the default algorithm for interoperability;
    // TOTP security does not depend on SHA-1 collision resistance.
    @SuppressWarnings("java:S4790")
    public String generateCode(String base32Seed, long timeIndex) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] key = base32Decode(base32Seed);
        byte[] counter = new byte[8];
        long time = timeIndex;
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) time;
            time >>>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(counter);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int code = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format(Locale.ROOT, "%0" + CODE_DIGITS + "d", code);
    }

    static String base32Encode(byte[] data) {
        StringBuilder encoded = new StringBuilder();
        int bits = 0;
        int buffer = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                encoded.append(BASE32_ALPHABET.charAt((buffer >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            encoded.append(BASE32_ALPHABET.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return encoded.toString();
    }

    static byte[] base32Decode(String base32) {
        String normalized = base32.toUpperCase(Locale.ROOT);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < normalized.length(); i++) {
            int value = BASE32_ALPHABET.indexOf(normalized.charAt(i));
            if (value < 0) {
                continue;
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
