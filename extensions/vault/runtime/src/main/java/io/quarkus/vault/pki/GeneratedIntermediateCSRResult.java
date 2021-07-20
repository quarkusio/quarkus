package io.quarkus.vault.pki;

import io.quarkus.vault.VaultPKISecretEngine;

/**
 * Result of {@link VaultPKISecretEngine#generateIntermediateCSR(GenerateIntermediateCSROptions)}.
 */
public class GeneratedIntermediateCSRResult {

    /**
     * Certificate Signing Request (PEM encoded).
     */
    public String csr;

    /**
     * Type of generated private key.
     */
    public CertificateKeyType privateKeyType;

    /**
     * Generated private key (PEM Encoded).
     * <p>
     * Only valid if {@link GenerateIntermediateCSROptions#exportPrivateKey} was true.
     */
    public String privateKey;

    public GeneratedIntermediateCSRResult setCsr(String csr) {
        this.csr = csr;
        return this;
    }

    public GeneratedIntermediateCSRResult setPrivateKeyType(CertificateKeyType privateKeyType) {
        this.privateKeyType = privateKeyType;
        return this;
    }

    public GeneratedIntermediateCSRResult setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }
}
