package com.securetransfer.transfer;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

/** Staff transfer: both accounts, a mandatory reason, a type and optional metadata. */
public record OperationsTransferRequest(
        @NotBlank
        @Pattern(regexp = TransferValidation.UUID_REGEX, message = "must be a valid UUID")
        String transactionRef,

        @NotNull
        TransferType type,

        @NotBlank
        @Pattern(regexp = TransferValidation.ACCOUNT_NUMBER_REGEX, message = "must be 10 digits")
        String sourceAccountNumber,

        @NotBlank
        @Pattern(regexp = TransferValidation.ACCOUNT_NUMBER_REGEX, message = "must be 10 digits")
        String destinationAccountNumber,

        @NotNull
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount,

        @NotBlank
        @Size(min = 10, max = 255)
        String reason,

        Map<String, Object> metadata) {
}
