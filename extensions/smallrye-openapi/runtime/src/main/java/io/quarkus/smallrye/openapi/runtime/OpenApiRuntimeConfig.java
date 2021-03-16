package io.quarkus.smallrye.openapi.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-openapi", phase = ConfigPhase.RUN_TIME)
public class OpenApiRuntimeConfig {

    /**
     * Enable the openapi endpoint. By default it's enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enable;

}
