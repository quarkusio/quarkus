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

    public GeneratedCertificate setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public GeneratedCertificate setCertificate(String certificate) {
        this.certificate = certificate;
        return this;
    }

    public GeneratedCertificate setIssuingCA(String issuingCA) {
        this.issuingCA = issuingCA;
        return this;
    }

    public GeneratedCertificate setCaChain(List<String> caChain) {
        this.caChain = caChain;
        return this;
    }

    public GeneratedCertificate setPrivateKeyType(CertificateKeyType privateKeyType) {
        this.privateKeyType = privateKeyType;
        return this;
    }

    public GeneratedCertificate setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }
}
