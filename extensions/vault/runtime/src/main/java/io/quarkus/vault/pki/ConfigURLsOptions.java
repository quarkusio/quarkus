package io.quarkus.vault.pki;

import java.util.List;

/**
 * Options for configuring URLs
 */
public class ConfigURLsOptions {

    /**
     * Specifies the URL values for the Issuing Certificate field.
     */
    public List<String> issuingCertificates;

    /**
     * Specifies the URL values for the CRL Distribution Points field.
     */
    public List<String> crlDistributionPoints;

    /**
     * Specifies the URL values for the OCSP Servers field.
     */
    public List<String> ocspServers;

}
