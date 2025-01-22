package com.htphatz.post_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    POST_NOT_FOUND(404, "Post not found", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND(404, "Comment not found", HttpStatus.NOT_FOUND),
    REACTION_NOT_FOUND(404, "Reaction not found", HttpStatus.NOT_FOUND),
    REACTION_CONFLICT(409, "Reaction conflict", HttpStatus.CONFLICT)
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
