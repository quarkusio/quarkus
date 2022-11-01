package io.quarkus.arc.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ArcContextPropagationConfig {

    /**
     * Support for context propagation will be enabled if the {@code quarkus-smallrye-context-propagation} extension is present,
     * and this value is {@code true}. If this value is unset then the support will be enabled unless the {@code quarkus-vertx}
     * extension is present and the {@code io.quarkus.vertx.runtime.VertxCurrentContextFactory} is registered.
     */
    @ConfigItem
    public Optional<Boolean> enabled;

}
