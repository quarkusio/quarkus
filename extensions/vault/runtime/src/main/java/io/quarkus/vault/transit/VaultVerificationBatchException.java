package io.quarkus.vault.transit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.transit.VerificationResult;

/**
 * Batch exception thrown from {@link VaultTransitSecretEngine#verifySignature(String, List)} if any error occurs.
 * The exception contains a map of errors with the associated message, and a list of successful verifications.
 * Note that a signature not matching an input is considered as an error, and will throw an exception.
 */
public class VaultVerificationBatchException extends VaultBatchException {

    private Map<VerificationRequest, VerificationResult> results;

    public VaultVerificationBatchException(String message, Map<VerificationRequest, VerificationResult> results) {
        super(message);
        this.results = results;
    }

    public Map<VerificationRequest, String> getErrors() {
        return getErrors(results);
    }

    public List<VerificationRequest> getValid() {
        return new ArrayList<>(getValid(results).keySet());
    }
}
