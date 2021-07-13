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

    /**
     * Specifies returned format of certificate data. If unspecified it defaults
     * to {@link DataFormat#PEM}
     */
    public DataFormat format;

    public SignIntermediateCAOptions setSubjectCommonName(String subjectCommonName) {
        this.subjectCommonName = subjectCommonName;
        return this;
    }

    public SignIntermediateCAOptions setSubjectOrganization(String subjectOrganization) {
        this.subjectOrganization = subjectOrganization;
        return this;
    }

    public SignIntermediateCAOptions setSubjectOrganizationalUnit(String subjectOrganizationalUnit) {
        this.subjectOrganizationalUnit = subjectOrganizationalUnit;
        return this;
    }

    public SignIntermediateCAOptions setSubjectStreetAddress(String subjectStreetAddress) {
        this.subjectStreetAddress = subjectStreetAddress;
        return this;
    }

    public SignIntermediateCAOptions setSubjectPostalCode(String subjectPostalCode) {
        this.subjectPostalCode = subjectPostalCode;
        return this;
    }

    public SignIntermediateCAOptions setSubjectLocality(String subjectLocality) {
        this.subjectLocality = subjectLocality;
        return this;
    }

    public SignIntermediateCAOptions setSubjectProvince(String subjectProvince) {
        this.subjectProvince = subjectProvince;
        return this;
    }

    public SignIntermediateCAOptions setSubjectCountry(String subjectCountry) {
        this.subjectCountry = subjectCountry;
        return this;
    }

    public SignIntermediateCAOptions setSubjectSerialNumber(String subjectSerialNumber) {
        this.subjectSerialNumber = subjectSerialNumber;
        return this;
    }

    public SignIntermediateCAOptions setSubjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions setExcludeCommonNameFromSubjectAlternativeNames(
            Boolean excludeCommonNameFromSubjectAlternativeNames) {
        this.excludeCommonNameFromSubjectAlternativeNames = excludeCommonNameFromSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions setIpSubjectAlternativeNames(
            List<String> ipSubjectAlternativeNames) {
        this.ipSubjectAlternativeNames = ipSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions setUriSubjectAlternativeNames(
            List<String> uriSubjectAlternativeNames) {
        this.uriSubjectAlternativeNames = uriSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions setOtherSubjectAlternativeNames(
            List<String> otherSubjectAlternativeNames) {
        this.otherSubjectAlternativeNames = otherSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions setTimeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
        return this;
    }

    public SignIntermediateCAOptions setMaxPathLength(Integer maxPathLength) {
        this.maxPathLength = maxPathLength;
        return this;
    }

    public SignIntermediateCAOptions setUseCSRValues(Boolean useCSRValues) {
        this.useCSRValues = useCSRValues;
        return this;
    }

    public SignIntermediateCAOptions setPermittedDnsDomains(List<String> permittedDnsDomains) {
        this.permittedDnsDomains = permittedDnsDomains;
        return this;
    }

    public SignIntermediateCAOptions setFormat(DataFormat format) {
        this.format = format;
        return this;
    }
}
