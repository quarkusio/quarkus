package io.quarkus.vault.transit;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.transit.EncryptionResult;

/**
 * Batch exception thrown from {@link VaultTransitSecretEngine#encrypt(String, List)} if any error occurs.
 * The exception contains a map of errors with the associated message, and a map of successful encryptions.
 */
public class VaultEncryptionBatchException extends VaultBatchException {

    private Map<EncryptionRequest, EncryptionResult> results;

    public VaultEncryptionBatchException(String message, Map<EncryptionRequest, EncryptionResult> results) {
        super(message);
        this.results = results;
    }

    public Map<EncryptionRequest, String> getErrors() {
        return getErrors(results);
    }

    public Map<EncryptionRequest, String> getValid() {
        return getValid(results);
    }
}
