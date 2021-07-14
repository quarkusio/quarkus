package io.quarkus.vault.pki;

/**
 * Options for enabling a new secret engine.
 */
public class EnableEngineOptions {

    /**
     * Default lease duration.
     */
    public String defaultLeaseTimeToLive;

    /**
     * Max lease duration.
     */
    public String maxLeaseTimeToLive;

}
