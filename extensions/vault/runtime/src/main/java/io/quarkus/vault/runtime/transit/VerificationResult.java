package io.quarkus.vault.runtime.transit;

public class VerificationResult extends VaultTransitBatchResult<Boolean> {

    public VerificationResult(boolean valid, String error) {
        super(valid, error);
    }

}
