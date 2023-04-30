package io.quarkus.jaeger.deployment.devservices;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class JaegerBuildTimeConfig {
    /**
     * Dev services configuration.
     */
    @ConfigItem
    public DevServicesConfig devservices;
}
