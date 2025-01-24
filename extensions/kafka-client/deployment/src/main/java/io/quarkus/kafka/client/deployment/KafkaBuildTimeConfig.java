package io.quarkus.kafka.client.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.kafka")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KafkaBuildTimeConfig {
    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     * <p>
     * If you enable the health check, you must specify the `kafka.bootstrap.servers` property.
     */
    @WithName("health.enabled")
    @WithDefault("false")
    boolean healthEnabled();

    /**
     * Whether to enable Snappy in native mode.
     * <p>
     * Note that Snappy requires GraalVM 21+ and embeds a native library in the native executable.
     * This library is unpacked and loaded when the application starts.
     */
    @WithName("snappy.enabled")
    @WithDefault("false")
    boolean snappyEnabled();

    /**
     * Whether to load the Snappy native library from the shared classloader.
     * This setting is only used in tests if the tests are using different profiles, which would lead to
     * unsatisfied link errors when loading Snappy.
     */
    @WithName("snappy.load-from-shared-classloader")
    @WithDefault("false")
    boolean snappyLoadFromSharedClassLoader();

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start Kafka in dev and test mode.
     */
    @ConfigDocSection(generated = true)
    KafkaDevServicesBuildTimeConfig devservices();

}
