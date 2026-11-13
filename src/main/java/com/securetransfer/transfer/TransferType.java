package com.securetransfer.transfer;

import com.securetransfer.security.PermissionCode;

/**
 * Set by the calling code path, never by a customer.
 * Staff types carry the permission required to post them through the operations endpoint.
 */
public enum TransferType {
    NORMAL_TRANSFER(null),
    RECONCILIATION(PermissionCode.TRANSFER_RECONCILE),
    AGENT_TRANSFER(PermissionCode.TRANSFER_AGENT);

    private final PermissionCode requiredPermission;

    TransferType(PermissionCode requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    /** Null means the type cannot be posted through the operations endpoint. */
    public PermissionCode requiredPermission() {
        return requiredPermission;
    }
}
