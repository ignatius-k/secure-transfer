package com.securetransfer.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 128) String password) {

    @Override
    public String toString() {
        return "TokenRequest[username=" + username + ", password=****]";   // never log passwords
    }
}
