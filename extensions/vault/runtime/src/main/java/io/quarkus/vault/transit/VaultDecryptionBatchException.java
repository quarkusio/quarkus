package io.quarkus.vault.transit;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.transit.DecryptionResult;

/**
 * Batch exception thrown from {@link VaultTransitSecretEngine#decrypt(String, List)} if any error occurs.
 * The exception contains a map of errors with the associated message, and a map of successful decryptions.
 */
public class VaultDecryptionBatchException extends VaultBatchException {

    private Map<DecryptionRequest, DecryptionResult> results;

    public VaultDecryptionBatchException(String message, Map<DecryptionRequest, DecryptionResult> results) {
        super(message);
        this.results = results;
    }

    public Map<DecryptionRequest, String> getErrors() {
        return getErrors(results);
    }

    public Map<DecryptionRequest, ClearData> getValid() {
        return getValid(results);
    }
}
