package com.securetransfer.error;

import org.springframework.http.HttpStatus;

/** Base for all business exceptions that map directly to an HTTP error response. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String title;

    public ApiException(HttpStatus status, String errorCode, String title, String detail) {
        super(detail);
        this.status = status;
        this.errorCode = errorCode;
        this.title = title;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getTitle() { return title; }
}
