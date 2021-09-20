package io.quarkus.vault.pki;

import java.util.List;

/**
 * Options for generating a certificate issued by the engine's CA.
 */
public class GenerateCertificateOptions {

    /**
     * Specifies Common Name (CN) of the certificate's subject.
     */
    public String subjectCommonName;

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
     * The format is the same as OpenSSL: <oid>;<type>:<value> where the only current valid type is UTF8. This
     * can be a comma-delimited list or a JSON string slice. Must match allowed_other_sans specified on the role.
     */
    public List<String> otherSubjectAlternativeNames;

    /**
     * Specifies request time-to-live. If not specified, the role's TTL will be used.
     * <p>
     * Value is specified as a string duration with time suffix. Hour is the largest supported suffix.
     */
    public String timeToLive;

    /**
     * Specifies returned format of certificate & private key data. If unspecified it defaults
     * to {@link DataFormat#PEM}
     */
    public DataFormat format;

    /**
     * Specifies encoding of private key data. If unspecified it defaults to {@link PrivateKeyEncoding#PKCS8}.
     */
    public PrivateKeyEncoding privateKeyEncoding;

    public GenerateCertificateOptions setSubjectCommonName(String subjectCommonName) {
        this.subjectCommonName = subjectCommonName;
        return this;
    }

    public GenerateCertificateOptions setSubjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
        return this;
    }

    public GenerateCertificateOptions setExcludeCommonNameFromSubjectAlternativeNames(
            Boolean excludeCommonNameFromSubjectAlternativeNames) {
        this.excludeCommonNameFromSubjectAlternativeNames = excludeCommonNameFromSubjectAlternativeNames;
        return this;
    }

    public GenerateCertificateOptions setIpSubjectAlternativeNames(
            List<String> ipSubjectAlternativeNames) {
        this.ipSubjectAlternativeNames = ipSubjectAlternativeNames;
        return this;
    }

    public GenerateCertificateOptions setUriSubjectAlternativeNames(
            List<String> uriSubjectAlternativeNames) {
        this.uriSubjectAlternativeNames = uriSubjectAlternativeNames;
        return this;
    }

    public GenerateCertificateOptions setOtherSubjectAlternativeNames(
            List<String> otherSubjectAlternativeNames) {
        this.otherSubjectAlternativeNames = otherSubjectAlternativeNames;
        return this;
    }

    public GenerateCertificateOptions setTimeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
        return this;
    }

    public GenerateCertificateOptions setFormat(DataFormat format) {
        this.format = format;
        return this;
    }

    public GenerateCertificateOptions setPrivateKeyEncoding(PrivateKeyEncoding privateKeyEncoding) {
        this.privateKeyEncoding = privateKeyEncoding;
        return this;
    }
}
