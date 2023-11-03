package io.quarkus.jaeger.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The Zipkin Jaeger configuration.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "jaeger.zipkin")
public class ZipkinConfig {

    /**
     * Whether jaeger should run in zipkin compatibility mode
     */
    @ConfigItem(defaultValue = "false")
    public Boolean compatibilityMode;
}
