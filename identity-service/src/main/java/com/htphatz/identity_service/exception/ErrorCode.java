package com.htphatz.identity_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_EXISTED(409, "User existed", HttpStatus.CONFLICT),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    PASSWORD_INVALID(400, "Invalid email/username or password", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    USER_CREATION_FAILED(500, "Failed to create user in Keycloak", HttpStatus.INTERNAL_SERVER_ERROR),
    UNCATEGORIZED_EXCEPTION(500, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(Integer code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
