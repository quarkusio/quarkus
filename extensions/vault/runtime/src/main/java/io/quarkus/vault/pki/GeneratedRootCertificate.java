package io.quarkus.vault.pki;

import io.quarkus.vault.VaultPKISecretEngine;

/**
 * Result of {@link VaultPKISecretEngine#generateRoot(GenerateRootOptions)}.
 */
public class GeneratedRootCertificate {

    /**
     * Serial number of generated certificate.
     */
    public String serialNumber;

    /**
     * Generated certificate (PEM encoded).
     */
    public String certificate;

    /**
     * Issuing CA of generated certificate (PEM encoded).
     */
    public String issuingCA;

    /**
     * Type of generated private key
     */
    public CertificateKeyType privateKeyType;

    /**
     * Generated private Key (PEM Encoded).
     */
    public String privateKey;

}
