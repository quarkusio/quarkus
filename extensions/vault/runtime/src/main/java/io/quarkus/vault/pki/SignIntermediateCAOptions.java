package io.quarkus.vault.pki;

import java.util.List;

/**
 * Options for signing an intermediate CA certificate.
 */
public class SignIntermediateCAOptions {

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
     * Specifies the maximum path length for generated certificate.
     */
    public Integer maxPathLength;

    /**
     * Flag determining if CSR values are used instead of configured default values.
     * <p>
     * Enables the following handling:
     * <ul>
     * <li>Subject information, including names and alternate names, will be preserved from the CSR.</li>
     * <li>Any key usages (for instance, non-repudiation) requested in the CSR will be added to the set of CA key
     * usages.</li>
     * <li>Extensions requested in the CSR will be copied into the issued certificate.</li>
     * </ul>
     */
    public Boolean useCSRValues;

    /**
     * DNS domains for which certificates are allowed to be issued or signed by this CA certificate. Subdomains
     * are allowed, as per RFC.
     */
    public List<String> permittedDnsDomains;
}
