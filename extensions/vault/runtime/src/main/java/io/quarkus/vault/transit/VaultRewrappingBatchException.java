package io.quarkus.vault.transit;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.transit.EncryptionResult;

/**
 * Batch exception thrown from {@link VaultTransitSecretEngine#rewrap(String, List)} if any error occurs.
 * The exception contains a map of errors with the associated message, and a map of successful reencryptions.
 */
public class VaultRewrappingBatchException extends VaultBatchException {

    private Map<RewrappingRequest, EncryptionResult> results;

    public VaultRewrappingBatchException(String message, Map<RewrappingRequest, EncryptionResult> results) {
        super(message);
        this.results = results;
    }

    public Map<RewrappingRequest, String> getErrors() {
        return getErrors(results);
    }

    public Map<RewrappingRequest, String> getValid() {
        return getValid(results);
    }
}
