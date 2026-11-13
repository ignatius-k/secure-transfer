package com.securetransfer.error;

import com.securetransfer.common.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

final class ApiErrors {

    private ApiErrors() {
    }

    static ApiError of(HttpStatus status, String errorCode, String title, String detail, String path) {
        return of(status, errorCode, title, detail, path, null, null);
    }

    static ApiError of(HttpStatus status, String errorCode, String title, String detail, String path,
                       String transactionRef, List<ApiError.FieldError> errors) {
        return new ApiError(title, status.value(), detail, path, errorCode, transactionRef,
                MDC.get(TraceIdFilter.MDC_KEY), Instant.now(), errors);
    }
}
