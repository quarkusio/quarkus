package org.jboss.shamrock.vertx.runtime;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

import java.util.Optional;

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
