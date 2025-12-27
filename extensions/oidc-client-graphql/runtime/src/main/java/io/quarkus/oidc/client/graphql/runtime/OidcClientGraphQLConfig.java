package io.quarkus.oidc.client.graphql.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.oidc-client-graphql")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OidcClientGraphQLConfig {

    /**
     * Name of the configured OidcClient used by GraphQL clients. You can override this configuration
     * for typesafe clients with the `io.quarkus.oidc.client.filter.OidcClientFilter` annotation,
     * or with the `quarkus.oidc-client-graphql."graphql-client-name".client-name` configuration property.
     */
    Optional<String> clientName();

    /**
     * OIDC client configuration for individual GraphQL clients.
     */
    @ConfigDocMapKey("graphql-client")
    @WithParentName
    Map<String, OidcClient> additionalOidcClients();

    /**
     * OIDC Client configuration.
     */
    interface OidcClient {

        /**
         * Name of the configured OidcClient used by the GraphQL client.
         */
        String clientName();

    }
}
