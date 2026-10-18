package io.quarkus.smallrye.graphql.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface SmallRyeGraphQLUIConfig {

    /**
     * The path where GraphQL UI is available.
     * The value `/` is not allowed as it blocks the application from serving anything else.
     * By default, this URL will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     */
    @WithDefault("graphql-ui")
    String rootPath();

    /**
     * Always include the UI. By default, this will only be included in dev and test.
     * Setting this to true will also include the UI in Prod
     */
    @WithDefault("false")
    boolean alwaysInclude();
}
