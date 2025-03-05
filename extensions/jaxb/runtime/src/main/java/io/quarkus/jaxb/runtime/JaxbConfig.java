package io.quarkus.jaxb.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.jaxb")
public interface JaxbConfig {

    /**
     * If enabled, it will validate the default JAXB context at build time.
     */
    @WithDefault("false")
    boolean validateJaxbContext();

    /**
     * Exclude classes to automatically be bound to the default JAXB context.
     * Values with suffix {@code .*}, i.e. {@code org.acme.*}, are considered packages and exclude all classes that are members
     * of these packages
     */
    Optional<List<String>> excludeClasses();
}
