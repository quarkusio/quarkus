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
     * Generated certificate.
     */
    public CertificateData certificate;

    /**
     * Issuing CA of generated certificate.
     */
    public CertificateData issuingCA;

    /**
     * Complete CA chain of generated certificate.
     */
    public List<CertificateData> caChain;

    /**
     * Type of generated private key
     */
    public CertificateKeyType privateKeyType;

    /**
     * Generated private Key (PEM Encoded).
     */
    public PrivateKeyData privateKey;

    public GeneratedCertificate setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public GeneratedCertificate setCertificate(CertificateData certificate) {
        this.certificate = certificate;
        return this;
    }

    public GeneratedCertificate setIssuingCA(CertificateData issuingCA) {
        this.issuingCA = issuingCA;
        return this;
    }

    public GeneratedCertificate setCaChain(List<CertificateData> caChain) {
        this.caChain = caChain;
        return this;
    }

    public GeneratedCertificate setPrivateKeyType(CertificateKeyType privateKeyType) {
        this.privateKeyType = privateKeyType;
        return this;
    }

    public GeneratedCertificate setPrivateKey(PrivateKeyData privateKey) {
        this.privateKey = privateKey;
        return this;
    }
}
