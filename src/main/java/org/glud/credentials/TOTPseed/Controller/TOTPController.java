package org.glud.credentials.TOTPseed.Controller;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.TOTPseed.Service.TOTPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/totp")
@RequiredArgsConstructor
public class TOTPController {

    @Autowired
    TOTPService totpService;


    @PostMapping("/generate-seed/{id}")
    public String generateSeed(@PathVariable String id) throws NoSuchAlgorithmException {
        return totpService.generateSeed(id);
    }



}
