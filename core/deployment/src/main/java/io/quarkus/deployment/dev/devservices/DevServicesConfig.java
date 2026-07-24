package io.quarkus.deployment.dev.devservices;

import java.time.Duration;
import java.util.List;
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

    /**
     * Container images for which license acceptance will be handled automatically.
     * <p>
     * Certain container images (e.g. MS SQL Server, IBM DB2) require explicit license
     * acceptance before they can be started. When a Dev Services extension finds its container image
     * in this list, it sets the appropriate license environment variable on the container
     * (e.g. {@code ACCEPT_EULA=Y} for MS SQL Server, {@code LICENSE=accept} for DB2).
     * <p>
     * Example: {@code quarkus.devservices.license-acceptance=mcr.microsoft.com/mssql/server:2022-latest}
     */
    Optional<List<String>> licenseAcceptance();

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
