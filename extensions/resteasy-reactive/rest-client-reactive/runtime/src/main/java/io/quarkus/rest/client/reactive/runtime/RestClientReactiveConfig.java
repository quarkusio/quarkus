package io.quarkus.rest.client.reactive.runtime;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Legacy REST client reactive config.
 *
 * @deprecated use {@link RestClientsConfig} instead
 */
@Deprecated
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class RestClientReactiveConfig {

    /**
     * Default scope for Rest Client Reactive. Use `jakarta.enterprise.context.Dependent` for spec-compliant behavior
     */
    @Deprecated // Deprecated in favour of RestClientsConfig.scope
    @ConfigItem(name = "scope", defaultValue = "jakarta.enterprise.context.ApplicationScoped")
    public String scope;

    /**
     * By default, RESTEasy Reactive uses text/plain content type for String values
     * and application/json for everything else.
     *
     * MicroProfile Rest Client spec requires the implementations to always default to application/json.
     * This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec
     */
    @Deprecated // Deprecated in favour of RestClientsConfig.disableSmartProduces
    @ConfigItem(name = "disable-smart-produces", defaultValue = "false")
    public boolean disableSmartProduces;

    /**
     * Whether providers (filters, etc.) annotated with {@link jakarta.ws.rs.ext.Provider} should be
     * automatically registered for all the clients in the application.
     */
    @ConfigItem(name = "provider-autodiscovery", defaultValue = "true")
    public boolean providerAutodiscovery = true;
}
