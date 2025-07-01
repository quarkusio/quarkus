package org.acme.example.extension.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.example")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ExampleRuntimeConfig {
    /**
     * Whether the banner will be displayed
     */
    @WithDefault("none")
    String runtimeName();
}
