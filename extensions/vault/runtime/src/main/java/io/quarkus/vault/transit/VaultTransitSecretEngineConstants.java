package io.quarkus.vault.transit;

import java.util.List;

import io.quarkus.vault.VaultException;

public class VaultTransitSecretEngineConstants {

    /**
     * Message that will be used in {@link VaultException} and {@link VaultVerificationBatchException}
     * thrown from the verify methods
     * {@link io.quarkus.vault.VaultTransitSecretEngine#verifySignature(String, String, String)},
     * {@link io.quarkus.vault.VaultTransitSecretEngine#verifySignature(String, String, SigningInput, TransitContext)} and
     * {@link io.quarkus.vault.VaultTransitSecretEngine#verifySignature(String, List)}
     */
    public static final String INVALID_SIGNATURE = "invalid signature";

}
