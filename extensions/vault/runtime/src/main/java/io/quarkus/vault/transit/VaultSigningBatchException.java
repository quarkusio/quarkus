package io.quarkus.vault.transit;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.transit.SigningResult;

/**
 * Batch exception thrown from {@link VaultTransitSecretEngine#decrypt(String, List)} if any error occurs.
 * The exception contains a map of errors with the associated message, and a map of successful sign.
 */
public class VaultSigningBatchException extends VaultBatchException {

    private Map<SigningRequest, SigningResult> results;

    public VaultSigningBatchException(String message, Map<SigningRequest, SigningResult> results) {
        super(message);
        this.results = results;
    }

    public Map<SigningRequest, String> getErrors() {
        return getErrors(results);
    }

    public Map<SigningRequest, String> getValid() {
        return getValid(results);
    }
}
