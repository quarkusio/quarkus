package io.quarkus.jaeger.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The Jaeger build time configuration.
 */
@ConfigRoot
public class JaegerBuildTimeConfig {
    /**
     * Defines if the Jaeger extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

}
