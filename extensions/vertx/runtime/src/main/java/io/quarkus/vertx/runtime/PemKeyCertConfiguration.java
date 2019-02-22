package io.quarkus.vertx.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PemKeyCertConfiguration {

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
