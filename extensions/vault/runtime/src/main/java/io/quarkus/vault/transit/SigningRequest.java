package io.quarkus.vault.transit;

import java.util.List;

import io.quarkus.vault.VaultTransitSecretEngine;

/**
 * A request to sign some arbitrary data, with optional key version and transit context.
 * 
 * @see VaultTransitSecretEngine#sign(String, List)
 */
public class SigningRequest extends VaultTransitBatchItem {

    private SigningInput input;
    private Integer keyVersion;

    public SigningRequest(SigningInput input) {
        this(input, null, null);
    }

    public SigningRequest(SigningInput input, TransitContext transitContext) {
        this(input, null, transitContext);
    }

    public SigningRequest(SigningInput input, Integer keyVersion) {
        this(input, keyVersion, null);
    }

    public SigningRequest(SigningInput input, Integer keyVersion, TransitContext transitContext) {
        super(transitContext);
        this.input = input;
        this.keyVersion = keyVersion;
    }

    public SigningInput getInput() {
        return input;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }
}
