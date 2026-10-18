package io.quarkus.smallrye.graphql.runtime;

import io.smallrye.config.WithDefault;

public interface SmallRyeGraphQLJsClientConfig {

    /**
     * Generate a JavaScript client proxy for all GraphQL operations.
     * When enabled, a static client library and a typed proxy module are generated
     * as web resources, along with an import map for bare module imports.
     */
    @WithDefault("false")
    boolean enabled();
}
