package com.tutipal.global.exception;

import org.springframework.http.HttpStatus;

/** 서비스 계층에서 던지면 GlobalExceptionHandler 가 {status, message} 로 응답. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
