package io.quarkus.smallrye.graphql.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
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
     * Enable metrics
     */
    @ConfigItem(name = "metrics.enabled", defaultValue = "false")
    boolean metricsEnabled;

    /**
     * UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    SmallRyeGraphQLUIConfig ui;

}
