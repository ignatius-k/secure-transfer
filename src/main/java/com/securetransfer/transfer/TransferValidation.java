package com.securetransfer.transfer;

final class TransferValidation {
    static final String UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    static final String ACCOUNT_NUMBER_REGEX = "^\\d{10}$";

    private TransferValidation() {
    }
}
