package org.acme.anotherExample.extension.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.another-extension.extension")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface QuarkusAnotherExampleExtensionConfig {
    /**
     * A Simple example flag
     */
    @WithDefault("false")
    boolean enabled();
}
