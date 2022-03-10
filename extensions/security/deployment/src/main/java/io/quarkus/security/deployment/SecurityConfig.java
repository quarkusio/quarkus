package io.quarkus.security.deployment;

import java.util.List;
import java.util.Optional;

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
     * List of security providers to enable for reflection
     */
    @ConfigItem
    public Optional<List<String>> securityProviders;
}
