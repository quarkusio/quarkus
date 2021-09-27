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
     * Generated certificate.
     */
    public CertificateData certificate;

    /**
     * Issuing CA of generated certificate.
     */
    public CertificateData issuingCA;

    /**
     * Type of generated private key
     */
    public CertificateKeyType privateKeyType;

    /**
     * Generated private Key (PEM Encoded).
     */
    public PrivateKeyData privateKey;

    public GeneratedRootCertificate setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public GeneratedRootCertificate setCertificate(CertificateData certificate) {
        this.certificate = certificate;
        return this;
    }

    public GeneratedRootCertificate setIssuingCA(CertificateData issuingCA) {
        this.issuingCA = issuingCA;
        return this;
    }

    public GeneratedRootCertificate setPrivateKeyType(CertificateKeyType privateKeyType) {
        this.privateKeyType = privateKeyType;
        return this;
    }

    public GeneratedRootCertificate setPrivateKey(PrivateKeyData privateKey) {
        this.privateKey = privateKey;
        return this;
    }
}
