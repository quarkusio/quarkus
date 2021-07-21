package io.quarkus.vault.pki;

import java.util.List;

/**
 * Options for PKI roles.
 */
public class RoleOptions {

    /**
     * Specifies default request time-to-live.
     * <p>
     * Value is specified as a string duration with time suffix. Hour is the largest supported suffix.
     */
    public String timeToLive;

    /**
     * Specifies maximum allowed time-to-live.
     * <p>
     * Value is specified as a string duration with time suffix. Hour is the largest supported suffix.
     */
    public String maxTimeToLive;

    /**
     * Specifies if clients can request certificates for localhost as one of the requested common names.
     */
    public Boolean allowLocalhost;

    /**
     * Specifies domains allowed on issued certificates.
     */
    public List<String> allowedDomains;

    /**
     * Flag allowing templates to be used in {@link #allowedDomains}.
     *
     * @see <a href="https://www.vaultproject.io/docs/concepts/policies">ACL Path Templating</a>
     */
    public Boolean allowTemplatesInAllowedDomains;

    /**
     * Specifies if clients can request certificates matching the value of the actual domains themselves.
     */
    public Boolean allowBareDomains;

    /**
     * Specifies if clients can request certificates with a Common Name (CN) that is a subdomain of the domains
     * allowed by the other role options. This includes wildcard subdomains.
     */
    public Boolean allowSubdomains;

    /**
     * Allows names specified in {@link #allowedDomains} to contain glob patterns (e.g. ftp*.example.com).
     */
    public Boolean allowGlobsInAllowedDomains;

    /**
     * Specifies if clients can request any Common Name (CN).
     */
    public Boolean allowAnyName;

    /**
     * Specifies if only valid host names are allowed for Common Names, DNS Subject Alternative Names, and the host
     * part of email addresses.
     */
    public Boolean enforceHostnames;

    /**
     * Specifies if clients can request IP Subject Alternative Names.
     */
    public Boolean allowIpSubjectAlternativeNames;

    /**
     * Defines allowed URI Subject Alternative Names.
     * <p>
     * Values can contain glob patterns (e.g. spiffe://hostname/*).
     */
    public List<String> allowedUriSubjectAlternativeNames;

    /**
     * Defines allowed custom OID/UTF8-string Subject Alternative Names.
     * <p>
     * The format is the same as OpenSSL: <oid>;<type>:<value> where the only current valid type is UTF8.
     */
    public List<String> allowedOtherSubjectAlternativeNames;

    /**
     * Specifies if certificates are flagged for server use.
     */
    public Boolean serverFlag;

    /**
     * Specifies if certificates are flagged for client use.
     */
    public Boolean clientFlag;

    /**
     * Specifies if certificates are flagged for code signing use.
     */
    public Boolean codeSigningFlag;

    /**
     * Specifies if certificates are flagged for email protection use.
     */
    public Boolean emailProtectionFlag;

    /**
     * Specifies the type of private keys to generate and the type of key expected for submitted CSRs.
     */
    public CertificateKeyType keyType;

    /**
     * Specifies the number of bits to use for the generated keys.
     * <p>
     * If {@link #keyType} is {@link CertificateKeyType#EC}, this value must be specified as well.
     */
    public Integer keyBits;

    /**
     * Specifies the allowed key usage constraint on issued certificates.
     */
    public List<CertificateKeyUsage> keyUsages;

    /**
     * Specifies the allowed extended key usage constraint on issued certificates.
     */
    public List<CertificateExtendedKeyUsage> extendedKeyUsages;

    /**
     * Specifies extended key usage OIDs.
     */
    public List<String> extendedKeyUsageOIDs;

    /**
     * Flag determining if the Common Name in the CSR will be used instead of that specified in request data.
     * <p>
     * Only applies to certificates signed using
     * {@link io.quarkus.vault.VaultPKISecretEngine#signRequest(String, String, GenerateCertificateOptions)}
     */
    public Boolean useCSRCommonName;

    /**
     * Flag determining if the Subject Alternative Names in the CSR will be used instead of that specified in
     * request data.
     * <p>
     * Only applies to certificates signed using
     * {@link io.quarkus.vault.VaultPKISecretEngine#signRequest(String, String, GenerateCertificateOptions)}
     */
    public Boolean useCSRSubjectAlternativeNames;

    /**
     * Specifies Organization (O) of the subject on issued certificates.
     */
    public String subjectOrganization;

    /**
     * Specifies Organizational Unit (OU) of the subject on issued certificates.
     */
    public String subjectOrganizationalUnit;

    /**
     * Specifies Street Address of the subject on issued certificates.
     */
    public String subjectStreetAddress;

    /**
     * Specifies Postal Code of the subject on issued certificates.
     */
    public String subjectPostalCode;

    /**
     * Specifies Locality (L) of the subject on issued certificates.
     */
    public String subjectLocality;

    /**
     * Specifies Province (ST) of the subject on issued certificates.
     */
    public String subjectProvince;

    /**
     * Specifies Country (C) of the subject on issued certificates.
     */
    public String subjectCountry;

    /**
     * Specifies allowed Serial Number (SERIALNUMBER) values of the subject on issued certificates.
     */
    public List<String> allowedSubjectSerialNumbers;

    /**
     * Specifies if certificates issued/signed against this role will have Vault leases attached to them.
     */
    public Boolean generateLease;

    /**
     * Flag determining if certificates issued/signed against this role will be stored in the storage backend.
     */
    public Boolean noStore;

    /**
     * Flag determining if the Common Name (CN) field is required when generating a certificate.
     */
    public Boolean requireCommonName;

    /**
     * List of policy OIDs.
     */
    public List<String> policyOIDs;

    /**
     * Mark Basic Constraints valid when issuing non-CA certificates.
     */
    public Boolean basicConstraintsValidForNonCA;

    /**
     * Specifies the duration by which to backdate on issued certificates not-before.
     * <p>
     * Value is specified as a string duration with time suffix. Hour is the largest supported suffix.
     */
    public String notBeforeDuration;

    public RoleOptions setTimeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
        return this;
    }

    public RoleOptions setMaxTimeToLive(String maxTimeToLive) {
        this.maxTimeToLive = maxTimeToLive;
        return this;
    }

    public RoleOptions setAllowLocalhost(Boolean allowLocalhost) {
        this.allowLocalhost = allowLocalhost;
        return this;
    }

    public RoleOptions setAllowedDomains(List<String> allowedDomains) {
        this.allowedDomains = allowedDomains;
        return this;
    }

    public RoleOptions setAllowTemplatesInAllowedDomains(Boolean allowTemplatesInAllowedDomains) {
        this.allowTemplatesInAllowedDomains = allowTemplatesInAllowedDomains;
        return this;
    }

    public RoleOptions setAllowBareDomains(Boolean allowBareDomains) {
        this.allowBareDomains = allowBareDomains;
        return this;
    }

    public RoleOptions setAllowSubdomains(Boolean allowSubdomains) {
        this.allowSubdomains = allowSubdomains;
        return this;
    }

    public RoleOptions setAllowGlobsInAllowedDomains(Boolean allowGlobsInAllowedDomains) {
        this.allowGlobsInAllowedDomains = allowGlobsInAllowedDomains;
        return this;
    }

    public RoleOptions setAllowAnyName(Boolean allowAnyName) {
        this.allowAnyName = allowAnyName;
        return this;
    }

    public RoleOptions setEnforceHostnames(Boolean enforceHostnames) {
        this.enforceHostnames = enforceHostnames;
        return this;
    }

    public RoleOptions setAllowIpSubjectAlternativeNames(Boolean allowIpSubjectAlternativeNames) {
        this.allowIpSubjectAlternativeNames = allowIpSubjectAlternativeNames;
        return this;
    }

    public RoleOptions setAllowedUriSubjectAlternativeNames(
            List<String> allowedUriSubjectAlternativeNames) {
        this.allowedUriSubjectAlternativeNames = allowedUriSubjectAlternativeNames;
        return this;
    }

    public RoleOptions setAllowedOtherSubjectAlternativeNames(
            List<String> allowedOtherSubjectAlternativeNames) {
        this.allowedOtherSubjectAlternativeNames = allowedOtherSubjectAlternativeNames;
        return this;
    }

    public RoleOptions setServerFlag(Boolean serverFlag) {
        this.serverFlag = serverFlag;
        return this;
    }

    public RoleOptions setClientFlag(Boolean clientFlag) {
        this.clientFlag = clientFlag;
        return this;
    }

    public RoleOptions setCodeSigningFlag(Boolean codeSigningFlag) {
        this.codeSigningFlag = codeSigningFlag;
        return this;
    }

    public RoleOptions setEmailProtectionFlag(Boolean emailProtectionFlag) {
        this.emailProtectionFlag = emailProtectionFlag;
        return this;
    }

    public RoleOptions setKeyType(CertificateKeyType keyType) {
        this.keyType = keyType;
        return this;
    }

    public RoleOptions setKeyBits(Integer keyBits) {
        this.keyBits = keyBits;
        return this;
    }

    public RoleOptions setKeyUsages(List<CertificateKeyUsage> keyUsages) {
        this.keyUsages = keyUsages;
        return this;
    }

    public RoleOptions setExtendedKeyUsages(
            List<CertificateExtendedKeyUsage> extendedKeyUsages) {
        this.extendedKeyUsages = extendedKeyUsages;
        return this;
    }

    public RoleOptions setExtendedKeyUsageOIDs(List<String> extendedKeyUsageOIDs) {
        this.extendedKeyUsageOIDs = extendedKeyUsageOIDs;
        return this;
    }

    public RoleOptions setUseCSRCommonName(Boolean useCSRCommonName) {
        this.useCSRCommonName = useCSRCommonName;
        return this;
    }

    public RoleOptions setUseCSRSubjectAlternativeNames(Boolean useCSRSubjectAlternativeNames) {
        this.useCSRSubjectAlternativeNames = useCSRSubjectAlternativeNames;
        return this;
    }

    public RoleOptions setSubjectOrganization(String subjectOrganization) {
        this.subjectOrganization = subjectOrganization;
        return this;
    }

    public RoleOptions setSubjectOrganizationalUnit(String subjectOrganizationalUnit) {
        this.subjectOrganizationalUnit = subjectOrganizationalUnit;
        return this;
    }

    public RoleOptions setSubjectStreetAddress(String subjectStreetAddress) {
        this.subjectStreetAddress = subjectStreetAddress;
        return this;
    }

    public RoleOptions setSubjectPostalCode(String subjectPostalCode) {
        this.subjectPostalCode = subjectPostalCode;
        return this;
    }

    public RoleOptions setSubjectLocality(String subjectLocality) {
        this.subjectLocality = subjectLocality;
        return this;
    }

    public RoleOptions setSubjectProvince(String subjectProvince) {
        this.subjectProvince = subjectProvince;
        return this;
    }

    public RoleOptions setSubjectCountry(String subjectCountry) {
        this.subjectCountry = subjectCountry;
        return this;
    }

    public RoleOptions setAllowedSubjectSerialNumbers(List<String> allowedSubjectSerialNumbers) {
        this.allowedSubjectSerialNumbers = allowedSubjectSerialNumbers;
        return this;
    }

    public RoleOptions setGenerateLease(Boolean generateLease) {
        this.generateLease = generateLease;
        return this;
    }

    public RoleOptions setNoStore(Boolean noStore) {
        this.noStore = noStore;
        return this;
    }

    public RoleOptions setRequireCommonName(Boolean requireCommonName) {
        this.requireCommonName = requireCommonName;
        return this;
    }

    public RoleOptions setPolicyOIDs(List<String> policyOIDs) {
        this.policyOIDs = policyOIDs;
        return this;
    }

    public RoleOptions setBasicConstraintsValidForNonCA(Boolean basicConstraintsValidForNonCA) {
        this.basicConstraintsValidForNonCA = basicConstraintsValidForNonCA;
        return this;
    }

    public RoleOptions setNotBeforeDuration(String notBeforeDuration) {
        this.notBeforeDuration = notBeforeDuration;
        return this;
    }
}
