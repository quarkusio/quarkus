package io.quarkus.vault.runtime.transit;

public class SigningResult extends VaultTransitBatchResult<String> {

    public SigningResult(String signature, String error) {
        super(signature, error);
    }

}
