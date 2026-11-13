package com.securetransfer.security;

/**
 * The permission catalogue. Code is the source of truth: every value here is
 * synced into the permissions table at startup by {@link PermissionSync}.
 * Add a value next to the code that checks it.
 */
public enum PermissionCode {
    TRANSFER_READ("View any transfer"),
    ACCOUNT_READ("View any account"),
    TRANSFER_RECONCILE("Post RECONCILIATION transfers between any two accounts"),
    TRANSFER_AGENT("Post AGENT_TRANSFER transfers between any two accounts");

    private final String description;

    PermissionCode(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
