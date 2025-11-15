package com.yd.travelbot.global.exception.code.status;

import org.springframework.http.HttpStatus;

import com.yd.travelbot.global.exception.code.BaseCodeInterface;

public enum CommonStatus implements BaseCodeInterface {
    OK(HttpStatus.OK, "OK"),
    CREATED(HttpStatus.CREATED, "Created"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
    CONFLICT(HttpStatus.CONFLICT, "Conflict"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String message;

    CommonStatus(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public HttpStatus httpStatus() {
        return status;
    }
}


