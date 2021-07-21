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

    public GeneratedRootCertificate setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public GeneratedRootCertificate setCertificate(String certificate) {
        this.certificate = certificate;
        return this;
    }

    public GeneratedRootCertificate setIssuingCA(String issuingCA) {
        this.issuingCA = issuingCA;
        return this;
    }

    public GeneratedRootCertificate setPrivateKeyType(CertificateKeyType privateKeyType) {
        this.privateKeyType = privateKeyType;
        return this;
    }

    public GeneratedRootCertificate setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }
}
