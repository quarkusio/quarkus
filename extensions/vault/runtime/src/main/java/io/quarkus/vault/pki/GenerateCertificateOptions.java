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
}
