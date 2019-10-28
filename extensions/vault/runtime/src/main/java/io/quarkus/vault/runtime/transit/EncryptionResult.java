package io.quarkus.vault.runtime.transit;

public class EncryptionResult extends VaultTransitBatchResult<String> {

    public EncryptionResult(String ciphertext, String error) {
        super(ciphertext, error);
    }

}
