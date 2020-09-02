package io.quarkus.oidc.token.propagation.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-token-propagation", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcTokenPropagationConfig {
    /**
     * Enable TokenCredentialFilter for all the injected MP RestClient implementations.
     * If this property is disabled then TokenCredentialRequestFilter has to be registered as an MP RestClient provider.
     */
    @ConfigItem(defaultValue = "false")
    public boolean registerFilter;
}
