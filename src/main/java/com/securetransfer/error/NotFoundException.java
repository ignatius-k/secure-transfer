package com.securetransfer.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public static NotFoundException account() {
        return new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found", "The requested account was not found.");
    }

    public static NotFoundException transfer() {
        return new NotFoundException("TRANSFER_NOT_FOUND", "Transfer not found", "The requested transfer was not found.");
    }

    private NotFoundException(String errorCode, String title, String detail) {
        super(HttpStatus.NOT_FOUND, errorCode, title, detail);
    }
}
