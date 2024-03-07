package io.quarkus.oidc.client.filter.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.resteasy-client-oidc-filter")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OidcClientFilterConfig {
    /**
     * Enable OidcClientRequestFilter for all the injected MP RestClient implementations.
     * If this property is disabled then OidcClientRequestFilter has to be registered as an MP RestClient provider.
     */
    @WithDefault("false")
    boolean registerFilter();

    /**
     * Name of the configured OidcClient used by the OidcClientRequestFilter. You can override this configuration for
     * individual MP RestClient with the `io.quarkus.oidc.client.filter.OidcClientFilter` annotation.
     */
    Optional<String> clientName();
}
