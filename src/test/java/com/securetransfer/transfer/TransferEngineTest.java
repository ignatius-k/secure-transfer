package com.securetransfer.transfer;

import com.securetransfer.account.Account;
import com.securetransfer.account.AccountRepository;
import com.securetransfer.account.AccountStatus;
import com.securetransfer.error.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferEngineTest {

    private static final String SOURCE = "1000000001";
    private static final String DESTINATION = "1000000002";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransferEngine engine;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T10:15:30Z"), ZoneOffset.UTC);
        engine = new TransferEngine(accountRepository, transactionRepository, clock);
    }

    @Test
    void transfer_success_debitsAndCredits() {
        Account source = account(SOURCE, AccountStatus.ACTIVE, "1000.00");
        Account destination = account(DESTINATION, AccountStatus.ACTIVE, "200.00");
        givenAccounts(source, destination);
        TransferCommand command = command("300.00");
        when(transactionRepository.findByTransactionRef(command.transactionRef())).thenReturn(Optional.empty());

        TransferResult result = engine.execute(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.replayed()).isFalse();
        assertThat(source.getBalance()).isEqualByComparingTo("700.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("500.00");

        Transaction saved = captureSavedTransaction();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(saved.getTransactionRef()).isEqualTo(command.transactionRef());
        assertThat(saved.getSourceAccountNumber()).isEqualTo(SOURCE);
        assertThat(saved.getDestinationAccountNumber()).isEqualTo(DESTINATION);
        assertThat(saved.getAmount()).isEqualByComparingTo("300.00");
        assertThat(saved.getType()).isEqualTo(TransferType.NORMAL_TRANSFER);
        assertThat(saved.getFailureReason()).isNull();
    }

    @Test
    void transfer_insufficientFunds_failsWithoutChangingBalances() {
        Account source = account(SOURCE, AccountStatus.ACTIVE, "100.00");
        Account destination = account(DESTINATION, AccountStatus.ACTIVE, "200.00");
        givenAccounts(source, destination);
        TransferCommand command = command("300.00");
        when(transactionRepository.findByTransactionRef(command.transactionRef())).thenReturn(Optional.empty());

        TransferResult result = engine.execute(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(source.getBalance()).isEqualByComparingTo("100.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("200.00");

        Transaction saved = captureSavedTransaction();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(FailureReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void transfer_inactiveAccount_fails() {
        Account source = account(SOURCE, AccountStatus.ACTIVE, "1000.00");
        Account destination = account(DESTINATION, AccountStatus.INACTIVE, "200.00");
        givenAccounts(source, destination);
        TransferCommand command = command("300.00");
        when(transactionRepository.findByTransactionRef(command.transactionRef())).thenReturn(Optional.empty());

        TransferResult result = engine.execute(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.transaction().getFailureReason()).isEqualTo(FailureReason.ACCOUNT_INACTIVE);
        assertThat(source.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("200.00");
        assertThat(captureSavedTransaction().getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void transfer_sameAccount_rejected() {
        TransferCommand command = new TransferCommand(UUID.randomUUID().toString(), TransferType.NORMAL_TRANSFER,
                SOURCE, SOURCE, new BigDecimal("100.00"), null, null, "jane.doe", Map.of());

        assertThatThrownBy(() -> engine.execute(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be different");

        // Rejected before any lock or write: nothing is recorded for an invalid request.
        verify(accountRepository, never()).findByAccountNumberForUpdate(anyString());
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    // --- helpers ---

    private static Account account(String number, AccountStatus status, String balance) {
        return new Account(number, "Holder " + number, null, new BigDecimal(balance), status);
    }

    private void givenAccounts(Account source, Account destination) {
        when(accountRepository.findByAccountNumberForUpdate(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate(destination.getAccountNumber())).thenReturn(Optional.of(destination));
    }

    private static TransferCommand command(String amount) {
        return new TransferCommand(UUID.randomUUID().toString(), TransferType.NORMAL_TRANSFER,
                SOURCE, DESTINATION, new BigDecimal(amount), "Rent", null, "jane.doe", Map.of("channel", "API"));
    }

    private Transaction captureSavedTransaction() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
