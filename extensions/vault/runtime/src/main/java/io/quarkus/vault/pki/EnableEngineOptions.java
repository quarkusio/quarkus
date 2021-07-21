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

    public EnableEngineOptions setDefaultLeaseTimeToLive(String defaultLeaseTimeToLive) {
        this.defaultLeaseTimeToLive = defaultLeaseTimeToLive;
        return this;
    }

    public EnableEngineOptions setMaxLeaseTimeToLive(String maxLeaseTimeToLive) {
        this.maxLeaseTimeToLive = maxLeaseTimeToLive;
        return this;
    }
}
