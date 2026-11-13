package com.securetransfer.transfer;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Customer transfer. No source account: it is resolved from the caller's token. */
public record CustomerTransferRequest(
        @NotBlank
        @Pattern(regexp = TransferValidation.UUID_REGEX, message = "must be a valid UUID")
        String transactionRef,

        @NotBlank
        @Pattern(regexp = TransferValidation.ACCOUNT_NUMBER_REGEX, message = "must be 10 digits")
        String destinationAccountNumber,

        @NotNull
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 140)
        String narration) {
}
