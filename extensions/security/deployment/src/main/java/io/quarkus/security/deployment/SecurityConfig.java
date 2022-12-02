package io.quarkus.security.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public final class SecurityConfig {

    /**
     * Whether authorization is enabled in dev mode or not. In other launch modes authorization is always enabled.
     */
    @ConfigItem(name = "auth.enabled-in-dev-mode", defaultValue = "true")
    public boolean authorizationEnabledInDevMode;

    /**
     * List of security providers to register
     */
    @ConfigItem
    public Optional<Set<String>> securityProviders;

    /**
     * Security provider configuration
     */
    @ConfigItem
    public Map<String, String> securityProviderConfig;
}
