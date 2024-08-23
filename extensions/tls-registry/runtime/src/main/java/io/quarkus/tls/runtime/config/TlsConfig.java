package io.quarkus.tls.runtime.config;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.tls")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TlsConfig {

    String DEFAULT_NAME = "<default>";

    /**
     * The default TLS bucket configuration
     */
    @WithParentName
    Optional<TlsBucketConfig> defaultCertificateConfig();

    /**
     * Configures additional (named) TLS bucket configurations.
     */
    @WithParentName
    @ConfigDocMapKey("tls-bucket-name")
    Map<String, TlsBucketConfig> namedCertificateConfig();
}
