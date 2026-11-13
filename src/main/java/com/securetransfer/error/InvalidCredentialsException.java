package com.securetransfer.error;

import org.springframework.http.HttpStatus;

/** Deliberately identical for unknown user and wrong password (no username enumeration). */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials",
                "Username or password is incorrect.");
    }
}
