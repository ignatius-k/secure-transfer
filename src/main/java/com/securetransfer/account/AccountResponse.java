package com.securetransfer.account;

import java.time.Instant;

public record AccountResponse(
        String accountNumber,
        String accountName,
        AccountStatus status,
        String balance,
        Instant asOf) {
}
