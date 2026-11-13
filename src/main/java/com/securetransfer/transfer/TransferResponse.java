package com.securetransfer.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * One transfer representation. Customer views omit internal fields (reason, initiatedBy, metadata);
 * staff views include them. Amounts are strings to avoid floating-point issues in clients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponse(
        String transactionRef,
        TransactionStatus status,
        TransferType type,
        String direction,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String destinationAccountName,
        String amount,
        String narration,
        String reason,
        FailureReason failureReason,
        String initiatedBy,
        Map<String, Object> metadata,
        Instant createdAt) {
}
