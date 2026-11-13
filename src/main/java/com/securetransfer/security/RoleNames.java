package com.securetransfer.security;

public final class RoleNames {

    /** Default role for all customers. Gated by role, needs no permissions. */
    public static final String CUSTOMER = "CUSTOMER";

    /** Starter staff role. Seeded with default permissions only when first created. */
    public static final String OPERATIONS = "OPERATIONS";

    private RoleNames() {
    }
}
