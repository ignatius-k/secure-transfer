package com.securetransfer.transfer;

import com.securetransfer.error.ApiException;

/**
 * Thrown by the API layer (after the engine has committed) to turn a FAILED result
 * into an HTTP error. The FAILED record is already persisted.
 */
public class TransferFailedException extends ApiException {

    private final String transactionRef;
    private final boolean replayed;

    public TransferFailedException(Transaction transaction, boolean replayed) {
        super(transaction.getFailureReason().status(),
                transaction.getFailureReason().name(),
                transaction.getFailureReason().title(),
                transaction.getFailureReason().detail());
        this.transactionRef = transaction.getTransactionRef();
        this.replayed = replayed;
    }

    public String getTransactionRef() { return transactionRef; }
    public boolean isReplayed() { return replayed; }
}
