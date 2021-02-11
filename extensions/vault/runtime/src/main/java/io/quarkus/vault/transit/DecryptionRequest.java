package io.quarkus.vault.transit;

import java.util.List;

import io.quarkus.vault.VaultTransitSecretEngine;

/**
 * A request to decrypt a ciphertext, with an optional transit context.
 * 
 * @see VaultTransitSecretEngine#decrypt(String, List)
 */
public class DecryptionRequest extends VaultTransitBatchItem {

    private String ciphertext;

    public DecryptionRequest(String ciphertext) {
        this(ciphertext, null);
    }

    public DecryptionRequest(String ciphertext, TransitContext transitContext) {
        super(transitContext);
        this.ciphertext = ciphertext;
    }

    public String getCiphertext() {
        return ciphertext;
    }

}
