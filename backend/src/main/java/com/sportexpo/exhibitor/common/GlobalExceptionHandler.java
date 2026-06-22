package com.sportexpo.exhibitor.common;

import com.sportexpo.exhibitor.auth.AuthErrorCodes;
import com.sportexpo.exhibitor.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity
                .status(statusFor(exception))
                .body(ApiResponse.error(exception.getMessage(), exception.getCode()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException exception) {
        return ResponseEntity
                .status(statusFor(exception))
                .body(ApiResponse.error(exception.getMessage(), exception.getCode()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", "E_AUTH_ACCESS_DENIED"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    private HttpStatus statusFor(AuthException exception) {
        return switch (exception.getCode()) {
            case AuthErrorCodes.ACCOUNT_LOCKED -> HttpStatus.LOCKED;
            case AuthErrorCodes.USER_DISABLED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.UNAUTHORIZED;
        };
    }

    private HttpStatus statusFor(BusinessException exception) {
        if (exception.getCode() == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (exception.getCode()) {
            case "E_SALES_ASSIGNMENT_CONFLICT", "E_PAYMENT_STATUS_CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
