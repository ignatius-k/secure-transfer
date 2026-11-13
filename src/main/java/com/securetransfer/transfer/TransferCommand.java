package com.securetransfer.transfer;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Everything the engine needs to post a transfer. Built by the calling service;
 * initiatedBy must always come from the security context, never from a request body.
 */
public record TransferCommand(
        String transactionRef,
        TransferType type,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String narration,
        String reason,
        String initiatedBy,
        Map<String, Object> metadata) {
}
