package io.quarkus.spring.di.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.spring-di")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface SpringDiBuildTimeConfig {

    /**
     * Whether Spring DI is enabled **during the build**.
     * <p>
     * Turning this setting off will result in Quarkus completely ignoring beans annotated with Spring annotations
     */
    @WithDefault("true")
    boolean enabled();

}
