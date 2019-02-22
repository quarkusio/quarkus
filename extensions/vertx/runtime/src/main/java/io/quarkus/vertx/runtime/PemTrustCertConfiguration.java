package io.quarkus.vertx.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.util.Optional;

@ConfigGroup
public class PemTrustCertConfiguration {

    /**
     * Comma-separated list of the trust certificate files (Pem format).
     */
    @ConfigItem
    public Optional<String> certs;

}
