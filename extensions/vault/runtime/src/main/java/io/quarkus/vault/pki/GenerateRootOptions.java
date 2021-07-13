package io.quarkus.vault.pki;

import java.util.List;

/**
 * Options for generating a self-signed root CA.
 */
public class GenerateRootOptions {

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
     * The format is the same as OpenSSL: <oid>;<type>:<value> where the only current valid type is UTF8.
     */
    public List<String> otherSubjectAlternativeNames;

    /**
     * Specifies time-to-live.
     * <p>
     * Value is specified as a string duration with time suffix. Hour is the largest supported suffix.
     */
    public String timeToLive;

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
     * Flag determining if the generated private key should be exported or kept internally.
     */
    public boolean exportPrivateKey = false;

    /**
     * Specifies the maximum path length for generated certificate.
     */
    public Integer maxPathLength;

    /**
     * DNS domains for which certificates are allowed to be issued or signed by this CA certificate. Subdomains
     * are allowed, as per RFC.
     */
    public List<String> permittedDnsDomains;

    /**
     * Specifies returned format of certificate & private key data. If unspecified it defaults
     * to {@link DataFormat#PEM}
     */
    public DataFormat format;

    /**
     * Specifies encoding of private key data. If unspecified it defaults to {@link PrivateKeyEncoding#PKCS8}.
     */
    public PrivateKeyEncoding privateKeyEncoding;

    public GenerateRootOptions setSubjectCommonName(String subjectCommonName) {
        this.subjectCommonName = subjectCommonName;
        return this;
    }

    public GenerateRootOptions setSubjectOrganization(String subjectOrganization) {
        this.subjectOrganization = subjectOrganization;
        return this;
    }

    public GenerateRootOptions setSubjectOrganizationalUnit(String subjectOrganizationalUnit) {
        this.subjectOrganizationalUnit = subjectOrganizationalUnit;
        return this;
    }

    public GenerateRootOptions setSubjectStreetAddress(String subjectStreetAddress) {
        this.subjectStreetAddress = subjectStreetAddress;
        return this;
    }

    public GenerateRootOptions setSubjectPostalCode(String subjectPostalCode) {
        this.subjectPostalCode = subjectPostalCode;
        return this;
    }

    public GenerateRootOptions setSubjectLocality(String subjectLocality) {
        this.subjectLocality = subjectLocality;
        return this;
    }

    public GenerateRootOptions setSubjectProvince(String subjectProvince) {
        this.subjectProvince = subjectProvince;
        return this;
    }

    public GenerateRootOptions setSubjectCountry(String subjectCountry) {
        this.subjectCountry = subjectCountry;
        return this;
    }

    public GenerateRootOptions setSubjectSerialNumber(String subjectSerialNumber) {
        this.subjectSerialNumber = subjectSerialNumber;
        return this;
    }

    public GenerateRootOptions setSubjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
        return this;
    }

    public GenerateRootOptions setExcludeCommonNameFromSubjectAlternativeNames(
            Boolean excludeCommonNameFromSubjectAlternativeNames) {
        this.excludeCommonNameFromSubjectAlternativeNames = excludeCommonNameFromSubjectAlternativeNames;
        return this;
    }

    public GenerateRootOptions setIpSubjectAlternativeNames(List<String> ipSubjectAlternativeNames) {
        this.ipSubjectAlternativeNames = ipSubjectAlternativeNames;
        return this;
    }

    public GenerateRootOptions setUriSubjectAlternativeNames(List<String> uriSubjectAlternativeNames) {
        this.uriSubjectAlternativeNames = uriSubjectAlternativeNames;
        return this;
    }

    public GenerateRootOptions setOtherSubjectAlternativeNames(
            List<String> otherSubjectAlternativeNames) {
        this.otherSubjectAlternativeNames = otherSubjectAlternativeNames;
        return this;
    }

    public GenerateRootOptions setTimeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
        return this;
    }

    public GenerateRootOptions setKeyType(CertificateKeyType keyType) {
        this.keyType = keyType;
        return this;
    }

    public GenerateRootOptions setKeyBits(Integer keyBits) {
        this.keyBits = keyBits;
        return this;
    }

    public GenerateRootOptions setExportPrivateKey(boolean exportPrivateKey) {
        this.exportPrivateKey = exportPrivateKey;
        return this;
    }

    public GenerateRootOptions setMaxPathLength(Integer maxPathLength) {
        this.maxPathLength = maxPathLength;
        return this;
    }

    public GenerateRootOptions setPermittedDnsDomains(List<String> permittedDnsDomains) {
        this.permittedDnsDomains = permittedDnsDomains;
        return this;
    }

    public GenerateRootOptions setFormat(DataFormat format) {
        this.format = format;
        return this;
    }

    public GenerateRootOptions setPrivateKeyEncoding(PrivateKeyEncoding privateKeyEncoding) {
        this.privateKeyEncoding = privateKeyEncoding;
        return this;
    }
}
