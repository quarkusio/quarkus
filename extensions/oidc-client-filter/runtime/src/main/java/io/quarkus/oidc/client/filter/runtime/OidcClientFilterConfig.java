package io.quarkus.oidc.client.filter.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-client-filter", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcClientFilterConfig {
    /**
     * Enable OidcClientRequestFilter for all the injected MP RestClient implementations.
     * If this property is disabled then OidcClientRequestFilter has to be registered as an MP RestClient provider.
     */
    @ConfigItem(defaultValue = "false")
    public boolean registerFilter;

    /**
     * Name of the configured OidcClient.
     */
    @ConfigItem
    public Optional<String> clientName;
}
