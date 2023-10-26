package io.quarkus.oidc.client.graphql.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-client-graphql", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OidcClientGraphQLConfig {

    /**
     * Name of the configured OidcClient used by GraphQL clients. You can override this configuration
     * for typesafe clients with the `io.quarkus.oidc.client.filter.OidcClientFilter` annotation.
     */
    @ConfigItem
    public Optional<String> clientName;
}
