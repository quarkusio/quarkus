package io.quarkus.vault.transit;

import java.util.List;

import io.quarkus.vault.VaultTransitSecretEngine;

/**
 * A request to verify that a signature matches an input, with optional key version and transit context.
 * 
 * @see VaultTransitSecretEngine#verifySignature(String, List)
 */
public class VerificationRequest extends VaultTransitBatchItem {

    private String signature;
    private SigningInput input;

    public VerificationRequest(String signature, SigningInput input) {
        this(signature, input, null);
    }

    public VerificationRequest(String signature, SigningInput input, TransitContext transitContext) {
        super(transitContext);
        this.signature = signature;
        this.input = input;
    }

    public SigningInput getInput() {
        return input;
    }

    public String getSignature() {
        return signature;
    }
}
