package io.quarkus.oidc.client.graphql.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.oidc-client-graphql")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OidcClientGraphQLConfig {

    /**
     * Name of the configured OidcClient used by GraphQL clients. You can override this configuration
     * for typesafe clients with the `io.quarkus.oidc.client.filter.OidcClientFilter` annotation.
     */
    Optional<String> clientName();
}
