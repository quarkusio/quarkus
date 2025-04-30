package io.quarkus.deployment.dev.devservices;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Dev Services
 *
 * @deprecated Please, use {@link DevServicesConfig} instead.
 */
@Deprecated(forRemoval = true, since = "3.18")
@ConfigRoot(name = "devservices")
public class GlobalDevServicesConfig {

    /**
     * Global flag that can be used to disable all Dev Services. If this is set to false then Dev Services will not be used.
     */
    @ConfigItem(defaultValue = "true", generateDocumentation = false)
    boolean enabled;

    /**
     * Global flag that can be used to force the attachmment of Dev Services to shared network. Default is false.
     */
    @ConfigItem(defaultValue = "false", generateDocumentation = false)
    public boolean launchOnSharedNetwork;

    /**
     * The timeout for starting a container
     */
    @ConfigItem(generateDocumentation = false)
    public Optional<Duration> timeout;

    /**
     * @deprecated Please, use {@link DevServicesConfig.Enabled} instead.
     */
    @Deprecated(forRemoval = true, since = "3.18")
    public static class Enabled implements BooleanSupplier {

        final GlobalDevServicesConfig config;

        public Enabled(GlobalDevServicesConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
