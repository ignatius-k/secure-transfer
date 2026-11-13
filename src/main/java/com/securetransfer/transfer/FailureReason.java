package com.securetransfer.transfer;

import org.springframework.http.HttpStatus;

/** Business rejections. These are recorded as FAILED transactions. */
public enum FailureReason {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Account not found",
            "Source or destination account was not found."),
    ACCOUNT_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Account inactive",
            "Source or destination account is not active."),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds",
            "Source account balance is insufficient for this transfer.");

    private final HttpStatus status;
    private final String title;
    private final String detail;

    FailureReason(HttpStatus status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    public HttpStatus status() { return status; }
    public String title() { return title; }
    public String detail() { return detail; }
}
