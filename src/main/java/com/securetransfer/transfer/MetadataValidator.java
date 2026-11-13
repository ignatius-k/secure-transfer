package com.securetransfer.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securetransfer.error.BadRequestException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Metadata is opaque context: flat, small, and unable to shadow core transfer fields.
 * Anything the engine needs to act on must become a real column instead.
 */
@Component
class MetadataValidator {

    private static final int MAX_BYTES = 4096;
    private static final int MAX_ENTRIES = 20;
    private static final int MAX_STRING_LENGTH = 255;
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{1,50}$");
    private static final Set<String> RESERVED_KEYS = Set.of(
            "transactionRef", "type", "status", "amount", "sourceAccountNumber",
            "destinationAccountNumber", "initiatedBy", "reason", "narration");

    private final ObjectMapper objectMapper;

    MetadataValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void validate(Map<String, Object> metadata) {
        if (metadata == null) {
            return;
        }
        if (metadata.size() > MAX_ENTRIES) {
            throw BadRequestException.invalidMetadata("metadata may contain at most " + MAX_ENTRIES + " entries.");
        }
        metadata.forEach((key, value) -> {
            if (!KEY_PATTERN.matcher(key).matches()) {
                throw BadRequestException.invalidMetadata("metadata key '" + key + "' is not allowed.");
            }
            if (RESERVED_KEYS.contains(key)) {
                throw BadRequestException.invalidMetadata("metadata key '" + key + "' is reserved.");
            }
            boolean allowed = value instanceof Number || value instanceof Boolean
                    || (value instanceof String s && s.length() <= MAX_STRING_LENGTH);
            if (!allowed) {
                throw BadRequestException.invalidMetadata(
                        "metadata values must be strings (max " + MAX_STRING_LENGTH + "), numbers or booleans.");
            }
        });
        try {
            if (objectMapper.writeValueAsString(metadata).getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
                throw BadRequestException.invalidMetadata("metadata must not exceed " + MAX_BYTES + " bytes.");
            }
        } catch (JsonProcessingException e) {
            throw BadRequestException.invalidMetadata("metadata is not serialisable.");
        }
    }
}
