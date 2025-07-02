package org.acme.example.extension.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.example")
@ConfigRoot
public interface ExampleConfig {
    /**
     * name
     */
    @WithDefault("none")
    String name();
}
