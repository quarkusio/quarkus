package io.quarkus.rest.client.reactive.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Build time REST client config.
 */
@ConfigMapping(prefix = "quarkus.rest-client")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface RestClientReactiveConfig {

    /**
     * By default, RESTEasy Reactive uses text/plain content type for String values
     * and application/json for everything else.
     * <p>
     * MicroProfile Rest Client spec requires the implementations to always default to application/json.
     * This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec
     */
    @WithName("disable-smart-produces")
    @WithDefault("false")
    boolean disableSmartProduces();

    /**
     * Whether providers (filters, etc.) annotated with {@link jakarta.ws.rs.ext.Provider} should be
     * automatically registered for all the clients in the application.
     */
    @WithName("provider-autodiscovery")
    @WithDefault("true")
    boolean providerAutodiscovery();
}
