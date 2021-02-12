package io.quarkus.resteasy.reactive.server.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rest")
public class ResteasyReactiveServerConfig {

    /**
     * Set this to override the default path for JAX-RS resources if there are no
     * annotated application classes.
     */
    @ConfigItem(defaultValue = "/")
    String path;
}
