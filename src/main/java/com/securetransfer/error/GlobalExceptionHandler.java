package com.securetransfer.error;

import com.securetransfer.transfer.TransferFailedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Maps every exception to the single ApiError shape. Never leaks stack traces, SQL or balances. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRANSACTION_REF_CONSTRAINT = "uk_transactions_transaction_ref";

    @ExceptionHandler(TransferFailedException.class)
    ResponseEntity<ApiError> transferFailed(TransferFailedException ex, HttpServletRequest request) {
        ApiError error = ApiErrors.of(ex.getStatus(), ex.getErrorCode(), ex.getTitle(), ex.getMessage(),
                request.getRequestURI(), ex.getTransactionRef(), null);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (ex.isReplayed()) {
            builder.header("Idempotent-Replayed", "true");
        }
        return builder.body(error);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException ex, HttpServletRequest request) {
        return respond(ApiErrors.of(ex.getStatus(), ex.getErrorCode(), ex.getTitle(), ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return respond(ApiErrors.of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed",
                "Request contains invalid fields.", request.getRequestURI(), null, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(ApiErrors.of(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request",
                "Request body is missing or not valid JSON for this endpoint.", request.getRequestURI()));
    }

    /** Thrown by @PreAuthorize and by service-level permission checks. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(ApiErrors.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden",
                "You do not have permission to perform this action.", request.getRequestURI()));
    }

    /**
     * The unique constraint on transaction_ref is the last line of defence against duplicates:
     * a concurrent request with the same ref but a different payload ends up here.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (message.contains(TRANSACTION_REF_CONSTRAINT)) {
            DuplicateTransactionRefException dup = new DuplicateTransactionRefException();
            return api(dup, request);
        }
        log.error("Data integrity violation", ex);
        return internal(request);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ApiError> lockFailure(PessimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Lock acquisition failed", ex);
        return respond(ApiErrors.of(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_BUSY", "Service busy",
                "The account is busy. Retry with the same transactionRef.", request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(ApiErrors.of(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found",
                "No endpoint at this path.", request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(ApiErrors.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed",
                ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return internal(request);
    }

    private ResponseEntity<ApiError> internal(HttpServletRequest request) {
        return respond(ApiErrors.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal error",
                "An unexpected error occurred. Quote the traceId when contacting support.",
                request.getRequestURI()));
    }

    private static ResponseEntity<ApiError> respond(ApiError error) {
        return ResponseEntity.status(error.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(error);
    }
}
