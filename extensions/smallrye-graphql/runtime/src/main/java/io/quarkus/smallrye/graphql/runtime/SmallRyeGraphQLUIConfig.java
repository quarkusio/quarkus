package io.quarkus.smallrye.graphql.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SmallRyeGraphQLUIConfig {

    /**
     * The path where GraphQL UI is available.
     * The value `/` is not allowed as it blocks the application from serving anything else.
     * By default, this URL will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     */
    @ConfigItem(defaultValue = "graphql-ui")
    public String rootPath;

    /**
     * Always include the UI. By default this will only be included in dev and test.
     * Setting this to true will also include the UI in Prod
     */
    @ConfigItem(defaultValue = "false")
    public boolean alwaysInclude;
}
