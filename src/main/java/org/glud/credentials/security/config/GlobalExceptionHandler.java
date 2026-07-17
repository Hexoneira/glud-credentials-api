package org.glud.credentials.security.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    //Entiende mejor este método
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        // Esto ya se dispara automáticamente si usas @Valid en el controlador
        // y el RequestDTO falla sus @NotBlank. Solo dale forma al mensaje.
        return new ResponseEntity<>("Credenciales con formato inválido", HttpStatus.BAD_REQUEST);
    }

    public Exception handleJwtException(RuntimeException e) {
        return new Exception("Token JWT inválido", e);
    }
}
