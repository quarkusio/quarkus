package io.quarkus.jaxb.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "jaxb")
public class JaxbConfig {
    /**
     * Exclude classes to automatically be bound to the default JAXB context.
     */
    @ConfigItem
    public Optional<List<String>> excludeClasses;
}
