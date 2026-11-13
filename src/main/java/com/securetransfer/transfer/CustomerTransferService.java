package com.securetransfer.transfer;

import com.securetransfer.account.Account;
import com.securetransfer.account.AccountRepository;
import com.securetransfer.error.BadRequestException;
import com.securetransfer.error.NotFoundException;
import com.securetransfer.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Customer use case: one account per customer, so the source is always the caller's own account.
 * Not transactional itself: the engine opens and commits the transaction.
 */
@Service
public class CustomerTransferService {

    private static final Set<String> CHANNELS = Set.of("MOBILE", "WEB", "USSD", "API");

    private final AccountRepository accountRepository;
    private final TransferEngine transferEngine;

    public CustomerTransferService(AccountRepository accountRepository, TransferEngine transferEngine) {
        this.accountRepository = accountRepository;
        this.transferEngine = transferEngine;
    }

    public TransferOutcome transfer(CurrentUser user, CustomerTransferRequest request, String channel) {
        Account ownAccount = accountRepository.findByCustomerId(user.userId())
                .orElseThrow(NotFoundException::account);

        // Early check for a clear error; the engine re-checks it anyway.
        if (ownAccount.getAccountNumber().equals(request.destinationAccountNumber())) {
            throw BadRequestException.sameAccount();
        }

        TransferCommand command = new TransferCommand(
                request.transactionRef(),
                TransferType.NORMAL_TRANSFER,
                ownAccount.getAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.narration(),
                null,
                user.username(),
                Map.of("channel", normaliseChannel(channel)));   // server-set, never client-supplied

        TransferResult result = transferEngine.execute(command);
        if (!result.isSuccess()) {
            throw new TransferFailedException(result.transaction(), result.replayed());
        }
        return new TransferOutcome(result, ownAccount.getAccountNumber());
    }

    private static String normaliseChannel(String channel) {
        if (channel == null) {
            return "API";
        }
        String upper = channel.trim().toUpperCase();
        return CHANNELS.contains(upper) ? upper : "API";
    }

    public record TransferOutcome(TransferResult result, String customerAccountNumber) {
    }
}
