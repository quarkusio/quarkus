package io.quarkus.vault.pki;

import java.util.List;

import io.quarkus.vault.VaultPKISecretEngine;

/**
 * Result of {@link VaultPKISecretEngine#generateCertificate(String, GenerateCertificateOptions)}.
 */
public class GeneratedCertificate {

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
     * Complete CA chain of generated certificate (elements are PEM encoded).
     */
    public List<String> caChain;

    /**
     * Type of generated private key
     */
    public CertificateKeyType privateKeyType;

    /**
     * Generated private Key (PEM Encoded).
     */
    public String privateKey;
}
