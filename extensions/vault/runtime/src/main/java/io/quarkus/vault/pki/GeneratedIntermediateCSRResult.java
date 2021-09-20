package io.quarkus.vault.pki;

import io.quarkus.vault.VaultPKISecretEngine;

/**
 * Result of {@link VaultPKISecretEngine#generateIntermediateCSR(GenerateIntermediateCSROptions)}.
 */
public class GeneratedIntermediateCSRResult {

    /**
     * Certificate Signing Request.
     */
    public CSRData csr;

    /**
     * Type of generated private key.
     */
    public CertificateKeyType privateKeyType;

    /**
     * Generated private key.
     * <p>
     * Only valid if {@link GenerateIntermediateCSROptions#exportPrivateKey} was true.
     */
    public PrivateKeyData privateKey;

    public GeneratedIntermediateCSRResult setCsr(CSRData csr) {
        this.csr = csr;
        return this;
    }

    public GeneratedIntermediateCSRResult setPrivateKeyType(CertificateKeyType privateKeyType) {
        this.privateKeyType = privateKeyType;
        return this;
    }

    public GeneratedIntermediateCSRResult setPrivateKey(PrivateKeyData privateKey) {
        this.privateKey = privateKey;
        return this;
    }
}
