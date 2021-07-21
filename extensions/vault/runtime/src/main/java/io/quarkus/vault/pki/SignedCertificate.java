package io.quarkus.vault.pki;

import java.util.List;

import io.quarkus.vault.VaultPKISecretEngine;

/**
 * Result of {@link VaultPKISecretEngine#signRequest(String, String, GenerateCertificateOptions)}.
 */
public class SignedCertificate {

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

    public SignedCertificate setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public SignedCertificate setCertificate(String certificate) {
        this.certificate = certificate;
        return this;
    }

    public SignedCertificate setIssuingCA(String issuingCA) {
        this.issuingCA = issuingCA;
        return this;
    }

    public SignedCertificate setCaChain(List<String> caChain) {
        this.caChain = caChain;
        return this;
    }
}
