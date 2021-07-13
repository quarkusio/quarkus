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
    public CertificateData certificate;

    /**
     * Issuing CA of generated certificate (PEM encoded).
     */
    public CertificateData issuingCA;

    /**
     * Complete CA chain of generated certificate (elements are PEM encoded).
     */
    public List<CertificateData> caChain;

    public SignedCertificate setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public SignedCertificate setCertificate(CertificateData certificate) {
        this.certificate = certificate;
        return this;
    }

    public SignedCertificate setIssuingCA(CertificateData issuingCA) {
        this.issuingCA = issuingCA;
        return this;
    }

    public SignedCertificate setCaChain(List<CertificateData> caChain) {
        this.caChain = caChain;
        return this;
    }
}
