package io.quarkus.vault.transit;

import java.util.List;

import io.quarkus.vault.VaultTransitSecretEngine;

/**
 * A request to rewrap a cipher text, with optional key version and transit context.
 * 
 * @see VaultTransitSecretEngine#rewrap(String, List)
 */
public class RewrappingRequest extends VaultTransitBatchItem {

    private String ciphertext;
    private Integer keyVersion;

    public RewrappingRequest(String ciphertext) {
        this(ciphertext, null, null);
    }

    public RewrappingRequest(String ciphertext, Integer keyVersion) {
        this(ciphertext, keyVersion, null);
    }

    public RewrappingRequest(String ciphertext, TransitContext transitContext) {
        this(ciphertext, null, transitContext);
    }

    public RewrappingRequest(String ciphertext, Integer keyVersion, TransitContext transitContext) {
        super(transitContext);
        this.ciphertext = ciphertext;
        this.keyVersion = keyVersion;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }
}
