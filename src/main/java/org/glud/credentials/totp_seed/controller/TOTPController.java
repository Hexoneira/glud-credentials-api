package org.glud.credentials.totp_seed.controller;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.totp_seed.service.TOTPService;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/totp")
@RequiredArgsConstructor
public class TOTPController {

    final TOTPService totpService;

    @PostMapping("/generate-seed/{id}")
    public String generateSeed(@PathVariable String id) throws NoSuchAlgorithmException {
        return totpService.generateSeed(id);
    }



}
