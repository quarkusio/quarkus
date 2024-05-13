package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.rest")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ResteasyReactiveServerConfig {

    /**
     * Set this to define the application path that serves as the base URI for all
     * JAX-RS resource URIs provided by {@code @Path} annotations when there are no
     * {@code @ApplicationPath} annotations defined on {@code Application} classes.
     * <p>
     * This value is always resolved relative to {@code quarkus.http.root-path}.
     */
    Optional<String> path();
}
