package com.securetransfer.transfer;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * SHA-256 over the business payload. Used to tell "same transactionRef, same request"
 * (safe replay) from "same transactionRef, different request" (409).
 */
final class RequestHasher {

    private RequestHasher() {
    }

    static String hash(TransferCommand cmd) {
        String canonical = String.join("|",
                String.valueOf(cmd.type()),
                cmd.sourceAccountNumber(),
                cmd.destinationAccountNumber(),
                cmd.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                nullSafe(cmd.narration()),
                nullSafe(cmd.reason()),
                cmd.initiatedBy(),
                canonicalMetadata(cmd.metadata()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String canonicalMetadata(Map<String, Object> metadata) {
        return metadata == null ? "" : new TreeMap<>(metadata).toString();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
