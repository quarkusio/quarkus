package io.quarkus.kafka.client.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kafka", phase = ConfigPhase.BUILD_TIME)
public class KafkaBuildTimeConfig {
    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     * <p>
     * If you enable the health check, you must specify the `kafka.bootstrap.servers` property.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "false")
    public boolean healthEnabled;

    /**
     * Whether to enable Snappy in native mode.
     * <p>
     * Note that Snappy requires GraalVM 21+ and embeds a native library in the native executable.
     * This library is unpacked and loaded when the application starts.
     */
    @ConfigItem(name = "snappy.enabled", defaultValue = "false")
    public boolean snappyEnabled;

    /**
     * Whether to load the Snappy native library from the shared classloader.
     * This setting is only used in tests if the tests are using different profiles, which would lead to
     * unsatisfied link errors when loading Snappy.
     */
    @ConfigItem(name = "snappy.load-from-shared-classloader", defaultValue = "false")
    public boolean snappyLoadFromSharedClassLoader;

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start Kafka in dev and test mode.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    public KafkaDevServicesBuildTimeConfig devservices;

}
