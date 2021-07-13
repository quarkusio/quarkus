package io.quarkus.vault.pki;

/**
 * Options for configuration the CRL
 */
public class ConfigCRLOptions {

    /**
     * Specifies the time until expiration.
     */
    public String expiry;

    /**
     * Disables or enables CRL building.
     */
    public Boolean disable;

}
