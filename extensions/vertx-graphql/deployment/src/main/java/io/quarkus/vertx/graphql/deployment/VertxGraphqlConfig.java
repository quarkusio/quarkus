package io.quarkus.vertx.graphql.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.vertx-graphql")
public interface VertxGraphqlConfig {

    /**
     * GraphQL UI configuration
     */
    VertxGraphqlUiConfig ui();

    interface VertxGraphqlUiConfig {
        /**
         * If GraphQL UI should be included every time. By default, this is only included when the application is running
         * in dev mode.
         */
        @WithDefault("false")
        boolean alwaysInclude();

        /**
         * The path where GraphQL UI is available.
         * <p>
         * The value `/` is not allowed as it blocks the application from serving anything else.
         */
        @WithDefault("graphql-ui")
        String path();
    }
}
