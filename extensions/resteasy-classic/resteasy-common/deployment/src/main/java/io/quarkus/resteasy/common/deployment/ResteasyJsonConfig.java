package io.quarkus.resteasy.common.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ResteasyJsonConfig {

    /**
     * If this is true (the default) then JSON is set to the default media type. If a method has no
     * produces/consumes and there is no builtin provider than can handle the type
     * then we will assume the response should be JSON.
     *
     * Note that this will only take effect if a JSON provider has been installed, such as quarkus-resteasy-jsonb
     * or quarkus-resteasy-jackson.
     */
    @ConfigItem(defaultValue = "true")
    public boolean jsonDefault;
}
