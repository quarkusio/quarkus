package io.quarkus.mongodb.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.mongodb")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface MongoClientBuildTimeConfig {
    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @WithDefault("true")
    @WithName("health.enabled")
    boolean healthEnabled();

    /**
     * Whether metrics are published in case a metrics extension is present.
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * If set to true, the default clients will always be created even if there are no injection points that use them
     */
    @WithName("force-default-clients")
    @WithDefault("false")
    boolean forceDefaultClients();

    /**
     * Whether tracing spans of driver commands are sent in case the quarkus-opentelemetry extension is present.
     */
    @WithName("tracing.enabled")
    @WithDefault("false")
    boolean tracingEnabled();

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start MongoDB in dev and test mode.
     */
    @ConfigDocSection(generated = true)
    DevServicesBuildTimeConfig devservices();
}
