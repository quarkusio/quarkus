package org.jboss.shamrock.vertx.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

@ConfigGroup
public class PemTrustCertConfiguration {

    /**
     * Comma-separated list of the trust certificate files (Pem format).
     */
    @ConfigItem
    public Optional<String> certs;

}
