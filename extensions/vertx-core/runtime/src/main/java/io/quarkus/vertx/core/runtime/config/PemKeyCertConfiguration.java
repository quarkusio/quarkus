package io.quarkus.vertx.core.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PemKeyCertConfiguration {

    /**
     * PEM Key/cert config is disabled by default.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "false")
    public boolean enabled;

    /**
     * Comma-separated list of the path to the key files (Pem format).
     */
    @ConfigItem
    public Optional<String> keys;

    /**
     * Comma-separated list of the path to the certificate files (Pem format).
     */
    @ConfigItem
    public Optional<String> certs;

}
