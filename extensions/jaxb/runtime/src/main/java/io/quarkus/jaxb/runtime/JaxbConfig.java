package io.quarkus.jaxb.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "jaxb")
public class JaxbConfig {

    /**
     * If enabled, it will validate the default JAXB context at build time.
     */
    @ConfigItem(defaultValue = "false")
    public boolean validateJaxbContext;

    /**
     * Exclude classes to automatically be bound to the default JAXB context.
     * Values with suffix {@code .*}, i.e. {@code org.acme.*}, are considered packages and exclude all classes that are members
     * of these packages
     */
    @ConfigItem
    public Optional<List<String>> excludeClasses;
}
