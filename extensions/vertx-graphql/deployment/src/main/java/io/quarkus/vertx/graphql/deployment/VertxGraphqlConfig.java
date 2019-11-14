package io.quarkus.vertx.graphql.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public final class VertxGraphqlConfig {
    /**
     * GraphQL UI configuration
     */
    @ConfigItem
    VertxGraphqlUiConfig ui;

    @ConfigGroup
    public static class VertxGraphqlUiConfig {
        /**
         * If GraphQL UI should be included every time. By default this is only included when the application is running
         * in dev mode.
         */
        @ConfigItem(defaultValue = "false")
        boolean alwaysInclude;

        /**
         * The path where GraphQL UI is available.
         * <p>
         * The value `/` is not allowed as it blocks the application from serving anything else.
         */
        @ConfigItem(defaultValue = "/graphql-ui")
        String path;
    }
}
