package io.quarkus.vertx.core.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PemKeyCertConfiguration {

    /**
     * PEM Key/cert config is disabled by default.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "false")
    public boolean enabled = false;

    /**
     * Comma-separated list of the path to the key files (Pem format).
     */
    @ConfigItem
    public Optional<List<String>> keys = Optional.empty();

    /**
     * Comma-separated list of the path to the certificate files (Pem format).
     */
    @ConfigItem
    public Optional<List<String>> certs = Optional.empty();

}
