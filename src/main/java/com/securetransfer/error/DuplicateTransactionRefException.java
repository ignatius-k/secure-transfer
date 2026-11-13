package com.securetransfer.error;

import org.springframework.http.HttpStatus;

/** Same transactionRef re-used with a different payload. */
public class DuplicateTransactionRefException extends ApiException {

    public DuplicateTransactionRefException() {
        super(HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION_REF", "Duplicate transaction reference",
                "This transactionRef has already been used for a different request.");
    }
}
