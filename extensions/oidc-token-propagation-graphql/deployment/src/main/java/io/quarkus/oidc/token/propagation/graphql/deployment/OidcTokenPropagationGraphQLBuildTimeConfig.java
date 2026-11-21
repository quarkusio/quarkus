package io.quarkus.oidc.token.propagation.graphql.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for OIDC token propagation to SmallRye GraphQL typesafe clients.
 */
@ConfigMapping(prefix = "quarkus.oidc-token-propagation-graphql")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface OidcTokenPropagationGraphQLBuildTimeConfig {
    /**
     * If the OIDC token propagation to GraphQL clients is enabled.
     * If disabled, the `@AccessToken` annotation on GraphQL clients is ignored.
     */
    @WithDefault("true")
    boolean enabled();
}
