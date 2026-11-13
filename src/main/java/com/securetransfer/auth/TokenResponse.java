package com.securetransfer.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** OAuth2 token response shape (RFC 6749 section 5.1), hence snake_case. */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn) {
}
