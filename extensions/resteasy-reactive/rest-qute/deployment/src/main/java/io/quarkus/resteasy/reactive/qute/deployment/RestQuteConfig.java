package io.quarkus.resteasy.reactive.qute.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rest.qute")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface RestQuteConfig {

    /**
     * If set to {@code true} then the {@link io.quarkus.qute.TemplateInstance} is registered as a non-blocking return type for
     * JAX-RS resource methods.
     *
     * @deprecated This config item will be removed at some time after Quarkus 3.16
     */
    @Deprecated(forRemoval = true, since = "3.10")
    @WithDefault("false")
    boolean templateInstanceNonBlockingType();

}
