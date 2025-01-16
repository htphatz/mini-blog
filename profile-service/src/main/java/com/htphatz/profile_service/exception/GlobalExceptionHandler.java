package com.htphatz.profile_service.exception;

import com.htphatz.profile_service.dto.response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage());
        String messages = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(messages)
                .build();
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("Conflict error: {}", e.getMessage());
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.CONFLICT.value())
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<APIResponse> handleRuntimeException(RuntimeException e) {
        log.error("Internal server error: {}", e.getMessage());
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<APIResponse> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        APIResponse apiResponse = APIResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<APIResponse> handleAuthenticationServiceException(AuthenticationServiceException e) {
        APIResponse apiResponse = APIResponse.builder()
                .code(401)
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(401).body(apiResponse);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<APIResponse> handleJwtException(JwtException e) {
        APIResponse apiResponse = APIResponse.builder()
                .code(401)
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(401).body(apiResponse);
    }
}
