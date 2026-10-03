package io.quarkus.oidc.token.propagation.graphql.runtime;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for OIDC token propagation to SmallRye GraphQL typesafe clients.
 */
@ConfigMapping(prefix = "quarkus.smallrye-graphql-client-oidc-token-propagation")
@ConfigRoot
public interface OidcTokenPropagationGraphQLBuildTimeConfig {
    /**
     * If the OIDC token propagation to GraphQL clients is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
