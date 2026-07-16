package org.glud.credentials.auth.controller;

import com.zaxxer.hikari.util.Credentials;
import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.service.UserService;
import org.glud.credentials.auth.utilities.RequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/api/auth/login")
    public ResponseEntity<String> credentials(@RequestBody RequestDTO request){
        return new ResponseEntity<>(userService.createResource(request), HttpStatus.CREATED);
        //Debo revisar si debo cambiar el tipo de retorno
    }
}
