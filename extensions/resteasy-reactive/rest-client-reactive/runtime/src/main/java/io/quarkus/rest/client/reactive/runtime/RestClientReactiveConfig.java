package io.quarkus.rest.client.reactive.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class RestClientReactiveConfig {

    /**
     * Default scope for Rest Client Reactive. Use `javax.enterprise.context.Dependent` for spec-compliant behavior
     */
    @ConfigItem(name = "scope", defaultValue = "javax.enterprise.context.ApplicationScoped")
    public String scope;

    /**
     * By default, RESTEasy Reactive uses text/plain content type for String values
     * and application/json for everything else.
     *
     * MicroProfile Rest Client spec requires the implementations to always default to application/json.
     * This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec
     */
    @ConfigItem(name = "disable-smart-produces", defaultValue = "false")
    public boolean disableSmartProduces;

    /**
     * Whether or not providers (filters, etc) annotated with {@link javax.ws.rs.ext.Provider} should be
     * automatically registered for all the clients in the application.
     */
    @ConfigItem(name = "provider-autodiscovery", defaultValue = "true")
    public boolean providerAutodiscovery = true;
}
