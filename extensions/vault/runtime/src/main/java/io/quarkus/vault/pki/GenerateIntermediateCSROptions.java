package io.quarkus.vault.pki;

import java.util.List;

/**
 * Options for generating a CSR for an intermediate CA.
 */
public class GenerateIntermediateCSROptions {

    /**
     * Specifies Common Name (CN) of the subject.
     */
    public String subjectCommonName;

    /**
     * Specifies Organization (O) of the subject.
     */
    public String subjectOrganization;

    /**
     * Specifies Organizational Unit (OU) of the subject.
     */
    public String subjectOrganizationalUnit;

    /**
     * Specifies Street Address of the subject.
     */
    public String subjectStreetAddress;

    /**
     * Specifies Postal Code of the subject.
     */
    public String subjectPostalCode;

    /**
     * Specifies Locality (L) of the subject.
     */
    public String subjectLocality;

    /**
     * Specifies Province (ST) of the subject.
     */
    public String subjectProvince;

    /**
     * Specifies Country (C) of the subject.
     */
    public String subjectCountry;

    /**
     * Specifies the Serial Number (SERIALNUMBER) of the subject.
     */
    public String subjectSerialNumber;

    /**
     * Specifies Subject Alternative Names.
     * <p>
     * These can be host names or email addresses; they will be parsed into their respective fields.
     */
    public List<String> subjectAlternativeNames;

    /**
     * Flag determining if the Common Name (CN) of the subject will be included
     * by default in the Subject Alternative Names of issued certificates.
     */
    public Boolean excludeCommonNameFromSubjectAlternativeNames;

    /**
     * Specifies IP Subject Alternative Names.
     */
    public List<String> ipSubjectAlternativeNames;

    /**
     * Specifies URI Subject Alternative Names.
     */
    public List<String> uriSubjectAlternativeNames;

    /**
     * Specifies custom OID/UTF8-string Subject Alternative Names.
     * <p>
     * The format is the same as OpenSSL: <oid>;<type>:<value> where the only current valid type is UTF8. Must match
     * {@link RoleOptions#allowedOtherSubjectAlternativeNames} specified on the role.
     */
    public List<String> otherSubjectAlternativeNames;

    /**
     * Specifies the desired type of private key to generate, RSA or EC.
     */
    public CertificateKeyType keyType;

    /**
     * Specifies the number of bits for the generated private key.
     * <p>
     * If {@link #keyType} is {@link CertificateKeyType#EC}, this value must be specified as well.
     */
    public Integer keyBits;

    /**
     * Specifies returned format of CSR & private key data. If unspecified it defaults
     * to {@link DataFormat#PEM}
     */
    public DataFormat format;

    /**
     * Specifies encoding of private key data. If unspecified it defaults to {@link PrivateKeyEncoding#PKCS8}.
     */
    public PrivateKeyEncoding privateKeyEncoding;

    /**
     * Flag determining if the generated private key should be exported or kept internally.
     */
    public boolean exportPrivateKey = false;

    public GenerateIntermediateCSROptions setSubjectCommonName(String subjectCommonName) {
        this.subjectCommonName = subjectCommonName;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectOrganization(String subjectOrganization) {
        this.subjectOrganization = subjectOrganization;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectOrganizationalUnit(String subjectOrganizationalUnit) {
        this.subjectOrganizationalUnit = subjectOrganizationalUnit;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectStreetAddress(String subjectStreetAddress) {
        this.subjectStreetAddress = subjectStreetAddress;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectPostalCode(String subjectPostalCode) {
        this.subjectPostalCode = subjectPostalCode;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectLocality(String subjectLocality) {
        this.subjectLocality = subjectLocality;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectProvince(String subjectProvince) {
        this.subjectProvince = subjectProvince;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectCountry(String subjectCountry) {
        this.subjectCountry = subjectCountry;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectSerialNumber(String subjectSerialNumber) {
        this.subjectSerialNumber = subjectSerialNumber;
        return this;
    }

    public GenerateIntermediateCSROptions setSubjectAlternativeNames(
            List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
        return this;
    }

    public GenerateIntermediateCSROptions setExcludeCommonNameFromSubjectAlternativeNames(
            Boolean excludeCommonNameFromSubjectAlternativeNames) {
        this.excludeCommonNameFromSubjectAlternativeNames = excludeCommonNameFromSubjectAlternativeNames;
        return this;
    }

    public GenerateIntermediateCSROptions setIpSubjectAlternativeNames(
            List<String> ipSubjectAlternativeNames) {
        this.ipSubjectAlternativeNames = ipSubjectAlternativeNames;
        return this;
    }

    public GenerateIntermediateCSROptions setUriSubjectAlternativeNames(
            List<String> uriSubjectAlternativeNames) {
        this.uriSubjectAlternativeNames = uriSubjectAlternativeNames;
        return this;
    }

    public GenerateIntermediateCSROptions setOtherSubjectAlternativeNames(
            List<String> otherSubjectAlternativeNames) {
        this.otherSubjectAlternativeNames = otherSubjectAlternativeNames;
        return this;
    }

    public GenerateIntermediateCSROptions setKeyType(CertificateKeyType keyType) {
        this.keyType = keyType;
        return this;
    }

    public GenerateIntermediateCSROptions setKeyBits(Integer keyBits) {
        this.keyBits = keyBits;
        return this;
    }

    public GenerateIntermediateCSROptions setExportPrivateKey(boolean exportPrivateKey) {
        this.exportPrivateKey = exportPrivateKey;
        return this;
    }

    public GenerateIntermediateCSROptions setFormat(DataFormat format) {
        this.format = format;
        return this;
    }

    public GenerateIntermediateCSROptions setPrivateKeyEncoding(
            PrivateKeyEncoding privateKeyEncoding) {
        this.privateKeyEncoding = privateKeyEncoding;
        return this;
    }
}
