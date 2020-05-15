package io.quarkus.smallrye.graphql.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-graphql")
public class SmallRyeGraphQLConfig {

    /**
     * The rootPath under which queries will be served. Default to /graphql
     */
    @ConfigItem(defaultValue = "/graphql")
    String rootPath;

    /**
     * The path where GraphQL UI is available.
     * The value `/` is not allowed as it blocks the application from serving anything else.
     */
    @ConfigItem(defaultValue = "/graphql-ui")
    String rootPathUi;

    /**
     * Always include the UI. By default this will only be included in dev and test.
     * Setting this to true will also include the UI in Prod
     */
    @ConfigItem(defaultValue = "false")
    boolean alwaysIncludeUi;

    /**
     * If GraphQL UI should be enabled. By default, GraphQL UI is enabled.
     */
    @ConfigItem(defaultValue = "true")
    boolean enableUi;

    /**
     * Enable metrics
     */
    @ConfigItem(name = "metrics.enabled", defaultValue = "false")
    public boolean metricsEnabled;

}
