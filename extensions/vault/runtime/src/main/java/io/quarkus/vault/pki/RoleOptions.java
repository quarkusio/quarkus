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
}
