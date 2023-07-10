package io.quarkus.opentelemetry.deployment.devservices;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.otel")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface OtelBuildTimeConfig {
    /**
     * Dev services configuration.
     */
    DevServicesConfig devservices();
}
