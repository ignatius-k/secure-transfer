package com.securetransfer.transfer;

/**
 * Outcome of {@link TransferEngine#execute}. replayed = true when the transactionRef had
 * already been processed with the same payload and the stored outcome is returned.
 */
public record TransferResult(Transaction transaction, boolean replayed) {

    public boolean isSuccess() {
        return transaction.getStatus() == TransactionStatus.SUCCESS;
    }
}
