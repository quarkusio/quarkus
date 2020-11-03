package io.quarkus.vertx.core.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PemTrustCertConfiguration {

    /**
     * PEM Trust config is disabled by default.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "false")
    public boolean enabled = false;

    /**
     * Comma-separated list of the trust certificate files (Pem format).
     */
    @ConfigItem
    public Optional<List<String>> certs = Optional.empty();

}
