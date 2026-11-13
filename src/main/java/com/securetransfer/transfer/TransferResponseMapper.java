package com.securetransfer.transfer;

import com.securetransfer.account.Account;
import com.securetransfer.account.AccountRepository;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class TransferResponseMapper {

    private final AccountRepository accountRepository;

    public TransferResponseMapper(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /** View for a customer who is a party to the transfer. Direction is relative to their account. */
    public TransferResponse customerView(Transaction t, String viewerAccountNumber) {
        String direction = viewerAccountNumber.equals(t.getSourceAccountNumber()) ? "DEBIT" : "CREDIT";
        return new TransferResponse(
                t.getTransactionRef(), t.getStatus(), t.getType(), direction,
                t.getSourceAccountNumber(), t.getDestinationAccountNumber(),
                mask(destinationName(t)),
                amount(t), t.getNarration(),
                null, t.getFailureReason(), null, null,
                t.getCreatedAt());
    }

    /** Full view for staff with TRANSFER_READ, or for the staff member who posted it. */
    public TransferResponse staffView(Transaction t) {
        return new TransferResponse(
                t.getTransactionRef(), t.getStatus(), t.getType(), null,
                t.getSourceAccountNumber(), t.getDestinationAccountNumber(),
                destinationName(t),
                amount(t), t.getNarration(),
                t.getReason(), t.getFailureReason(), t.getInitiatedBy(), t.getMetadata(),
                t.getCreatedAt());
    }

    private String destinationName(Transaction t) {
        return accountRepository.findByAccountNumber(t.getDestinationAccountNumber())
                .map(Account::getAccountName)
                .orElse(null);
    }

    private static String amount(Transaction t) {
        return t.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** "John Smith" -> "John S****": lets the sender confirm the recipient without exposing the full name. */
    static String mask(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + "****";
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + "****";
    }
}
