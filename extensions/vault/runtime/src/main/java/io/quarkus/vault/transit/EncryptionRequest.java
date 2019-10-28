package io.quarkus.vault.transit;

import java.util.List;

import io.quarkus.vault.VaultTransitSecretEngine;

/**
 * A request to encrypt some arbitrary data, with optional key version and transit context.
 * 
 * @see VaultTransitSecretEngine#encrypt(String, List)
 */
public class EncryptionRequest extends VaultTransitBatchItem {

    private ClearData data;
    private Integer keyVersion;

    public EncryptionRequest(ClearData data) {
        this(data, null, null);
    }

    public EncryptionRequest(ClearData data, Integer keyVersion) {
        this(data, keyVersion, null);
    }

    public EncryptionRequest(ClearData data, TransitContext transitContext) {
        this(data, null, transitContext);
    }

    public EncryptionRequest(ClearData data, Integer keyVersion, TransitContext transitContext) {
        super(transitContext);
        this.data = data;
        this.keyVersion = keyVersion;
    }

    public ClearData getData() {
        return data;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }
}
