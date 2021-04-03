package io.quarkus.rest.client.reactive;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME, name = "rest-client")
public class ReactiveResteasyMpClientConfig {

    /**
     * Default scope for MicroProfile Rest Client. Use `javax.enterprise.context.Dependent` for spec-compliant behavior
     */
    @ConfigItem(name = "scope", defaultValue = "javax.enterprise.context.ApplicationScoped")
    public String scope;
}