package io.quarkus.vertx.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PemTrustCertConfiguration {

    /**
     * Comma-separated list of the trust certificate files (Pem format).
     */
    @ConfigItem
    public Optional<String> certs;

}
