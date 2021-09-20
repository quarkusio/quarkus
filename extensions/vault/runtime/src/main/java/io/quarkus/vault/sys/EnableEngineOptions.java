package io.quarkus.vault.sys;

import java.util.Map;

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

    /**
     * Engine specific mount options
     */
    public Map<String, String> options;

    public EnableEngineOptions setDefaultLeaseTimeToLive(String defaultLeaseTimeToLive) {
        this.defaultLeaseTimeToLive = defaultLeaseTimeToLive;
        return this;
    }

    public EnableEngineOptions setMaxLeaseTimeToLive(String maxLeaseTimeToLive) {
        this.maxLeaseTimeToLive = maxLeaseTimeToLive;
        return this;
    }

    public EnableEngineOptions setOptions(Map<String, String> options) {
        this.options = options;
        return this;
    }
}
