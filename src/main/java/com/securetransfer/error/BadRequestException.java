package com.securetransfer.error;

import org.springframework.http.HttpStatus;

/** 400 errors raised by business validation (not bean validation). */
public class BadRequestException extends ApiException {

    public static BadRequestException sameAccount() {
        return new BadRequestException("SAME_ACCOUNT_TRANSFER", "Same account transfer",
                "Source and destination accounts must be different.");
    }

    public static BadRequestException invalidAmount() {
        return new BadRequestException("INVALID_AMOUNT", "Invalid amount",
                "Amount must be greater than zero with at most 2 decimal places.");
    }

    public static BadRequestException transferTypeNotAllowed(String type) {
        return new BadRequestException("TRANSFER_TYPE_NOT_ALLOWED", "Transfer type not allowed",
                "Transfer type " + type + " cannot be posted through this endpoint.");
    }

    public static BadRequestException invalidMetadata(String detail) {
        return new BadRequestException("INVALID_METADATA", "Invalid metadata", detail);
    }

    private BadRequestException(String errorCode, String title, String detail) {
        super(HttpStatus.BAD_REQUEST, errorCode, title, detail);
    }
}
