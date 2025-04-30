package io.quarkus.arc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface ArcContextPropagationConfig {

    /**
     * If set to true and the SmallRye Context Propagation extension is present then the CDI contexts will be propagated by
     * means of the MicroProfile Context Propagation API. Specifically, a
     * {@link org.eclipse.microprofile.context.spi.ThreadContextProvider} implementation is registered.
     *
     * On the other hand, if set to false then the MicroProfile Context Propagation API will never be used to propagate the CDI
     * contexts.
     *
     * Note that the CDI contexts may be propagated in a different way though. For example with the Vertx duplicated context.
     */
    @WithDefault("true")
    boolean enabled();

}
