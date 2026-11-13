package com.securetransfer.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Single error shape for every endpoint (RFC 9457 Problem Details fields, without "type").
 * errorCode is the stable, machine-readable contract; detail is human-readable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String title,
        int status,
        String detail,
        String instance,
        String errorCode,
        String transactionRef,
        String traceId,
        Instant timestamp,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }
}
