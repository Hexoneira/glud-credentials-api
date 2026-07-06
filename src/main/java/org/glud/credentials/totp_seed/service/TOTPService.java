package org.glud.credentials.totp_seed.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class TOTPService {

    @Value("${totp.tenant}")
    private String tenantCode;
    @Value("${totp.secret}")
    private String serverSecret;

    public String generateSeed(String  studentId) throws NoSuchAlgorithmException {
        String seed = studentId +":"+tenantCode+":"+serverSecret;

        byte[] hash = MessageDigest.getInstance("SHA-256").digest(seed.getBytes());

        return Base64.getEncoder().encodeToString(hash).substring(0,16);
    }

}
