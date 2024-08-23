package io.quarkus.security.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 *
 */
@ConfigMapping(prefix = "quarkus.security")
@ConfigRoot
public interface SecurityConfig {

    /**
     * Whether authorization is enabled in dev mode or not. In other launch modes authorization is always enabled.
     */
    @WithName("auth.enabled-in-dev-mode")
    @WithDefault("true")
    boolean authorizationEnabledInDevMode();

    /**
     * List of security providers to register
     */
    Optional<Set<String>> securityProviders();

    /**
     * Security provider configuration
     */
    @ConfigDocMapKey("provider-name")
    Map<String, String> securityProviderConfig();
}
