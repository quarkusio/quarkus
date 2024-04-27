package io.quarkus.security.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus Security configuration.
 */
@ConfigMapping(prefix = "quarkus.security")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SecurityConfig {

    /**
     * Security events configuration.
     */
    SecurityEventsConfig events();

    interface SecurityEventsConfig {

        /**
         * Whether security events should be fired.
         */
        @WithDefault("true")
        boolean enabled();

    }

}
