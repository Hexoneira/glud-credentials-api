package org.glud.credentials.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiError> handleTenantNotFound(TenantNotFoundException ex) {
        return error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiError> handleMemberNotFound(MemberNotFoundException ex) {
        return error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleTenantAlreadyExists(TenantAlreadyExistsException ex) {
        return error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TenantHasMembersException.class)
    public ResponseEntity<ApiError> handleTenantHasMembers(TenantHasMembersException ex) {
        return error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SuperAdminRequiredException.class)
    public ResponseEntity<ApiError> handleSuperAdminRequired(SuperAdminRequiredException ex) {
        return error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RoleRequiredException.class)
    public ResponseEntity<ApiError> handleRoleRequired(RoleRequiredException ex) {
        return error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CrossTenantAccessException.class)
    public ResponseEntity<ApiError> handleCrossTenantAccess(CrossTenantAccessException ex) {
        return error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MemberAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleMemberAlreadyExists(MemberAlreadyExistsException ex) {
        return error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidMemberActionException.class)
    public ResponseEntity<ApiError> handleInvalidMemberAction(InvalidMemberActionException ex) {
        return error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ActiveGuestAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleActiveGuestAlreadyExists(ActiveGuestAlreadyExistsException ex) {
        return error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(GuestLimitExceededException.class)
    public ResponseEntity<ApiError> handleGuestLimitExceeded(GuestLimitExceededException ex) {
        return error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        return error("Datos inválidos", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error("Cuerpo de la solicitud inválido", HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ApiError> error(String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiError(message), status);
    }
}
