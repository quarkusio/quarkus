package io.quarkus.deployment.dev.devservices;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Dev Services
 */
@ConfigMapping(prefix = "quarkus.devservices")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DevServicesConfig {

    /**
     * Global flag that can be used to disable all Dev Services. If this is set to false then Dev Services will not be used.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Global flag that can be used to force the attachment of Dev Services to shared network. Default is false.
     */
    @WithDefault("false")
    boolean launchOnSharedNetwork();

    /**
     * The timeout for starting a container
     */
    Optional<Duration> timeout();

    class Enabled implements BooleanSupplier {

        final DevServicesConfig config;

        public Enabled(DevServicesConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
