package com.securetransfer.transfer;

import com.securetransfer.account.Account;
import com.securetransfer.account.AccountRepository;
import com.securetransfer.error.BadRequestException;
import com.securetransfer.error.DuplicateTransactionRefException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The single place where money moves. Every transfer type, from every caller, is posted here.
 *
 * <p>The engine does not trust callers: it re-validates every rule itself, whatever the caller
 * has already checked. Callers never write to accounts or transactions directly.
 *
 * <p>Transaction boundary: {@code REQUIRED}. Called on its own, it runs in its own transaction.
 * Called from a caller that is already {@code @Transactional} (e.g. reconciliation), it joins that
 * transaction so the transfer and the caller's own records commit or roll back together.
 * It lives in its own bean so calls always go through the Spring transactional proxy.
 */
@Service
public class TransferEngine {

    private static final Logger log = LoggerFactory.getLogger(TransferEngine.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public TransferEngine(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public TransferResult execute(TransferCommand cmd) {
        // 1. Request-level rules: invalid requests are rejected and NOT recorded.
        validate(cmd);
        String requestHash = RequestHasher.hash(cmd);

        // 2. Lock both accounts in a fixed (sorted) order to prevent deadlocks
        //    between concurrent A->B and B->A transfers.
        Map<String, Account> accounts = lockInOrder(cmd.sourceAccountNumber(), cmd.destinationAccountNumber());

        // 3. Idempotency, checked AFTER locking: a concurrent request with the same ref and payload
        //    touches the same accounts, so it waits on the lock and then sees the committed record.
        //    A same-ref request with a different payload that slips past this check is stopped by
        //    the unique constraint on transaction_ref (mapped to 409).
        Optional<Transaction> existing = transactionRepository.findByTransactionRef(cmd.transactionRef());
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new DuplicateTransactionRefException();
            }
            log.info("Replaying transfer {}", cmd.transactionRef());
            return new TransferResult(existing.get(), true);
        }

        Account source = accounts.get(cmd.sourceAccountNumber());
        Account destination = accounts.get(cmd.destinationAccountNumber());
        Transaction transaction = Transaction.from(cmd, requestHash, Instant.now(clock));

        // 4. Business rules: checked BEFORE any balance changes, so a rejection
        //    has nothing to undo and is recorded as FAILED.
        FailureReason failure = checkBusinessRules(source, destination, cmd.amount());
        if (failure != null) {
            transaction.markFailed(failure);
            transactionRepository.saveAndFlush(transaction);
            log.info("Transfer {} FAILED: {}", cmd.transactionRef(), failure);
            return new TransferResult(transaction, false);
        }

        // 5. Post: debit, credit and record commit atomically. Any exception rolls back all three.
        source.debit(cmd.amount());
        destination.credit(cmd.amount());
        transaction.markSuccess();
        transactionRepository.saveAndFlush(transaction);
        log.info("Transfer {} SUCCESS: {} {} -> {}", cmd.transactionRef(), cmd.amount(),
                cmd.sourceAccountNumber(), cmd.destinationAccountNumber());
        return new TransferResult(transaction, false);
    }

    private void validate(TransferCommand cmd) {
        BigDecimal amount = cmd.amount();
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            throw BadRequestException.invalidAmount();
        }
        if (cmd.sourceAccountNumber().equals(cmd.destinationAccountNumber())) {
            throw BadRequestException.sameAccount();
        }
        if (cmd.type() == null || cmd.initiatedBy() == null || cmd.transactionRef() == null) {
            throw new IllegalArgumentException("TransferCommand is missing required fields");
        }
    }

    private Map<String, Account> lockInOrder(String a, String b) {
        Map<String, Account> locked = new HashMap<>();
        Stream.of(a, b).sorted().forEach(number ->
                accountRepository.findByAccountNumberForUpdate(number)
                        .ifPresent(account -> locked.put(number, account)));
        return locked;
    }

    private FailureReason checkBusinessRules(Account source, Account destination, BigDecimal amount) {
        if (source == null || destination == null) {
            return FailureReason.ACCOUNT_NOT_FOUND;
        }
        if (!source.isActive() || !destination.isActive()) {
            return FailureReason.ACCOUNT_INACTIVE;
        }
        if (!source.hasSufficientFunds(amount)) {
            return FailureReason.INSUFFICIENT_FUNDS;
        }
        return null;
    }
}
