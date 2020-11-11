package io.quarkus.smallrye.graphql.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.graphql.schema.helper.TypeAutoNameStrategy;

@ConfigRoot(name = "smallrye-graphql")
public class SmallRyeGraphQLConfig {

    /**
     * The rootPath under which queries will be served. Default to /graphql
     */
    @ConfigItem(defaultValue = "/graphql")
    String rootPath;

    /**
     * Enable metrics. By default this will be enabled if the metrics extension is added.
     */
    @ConfigItem(name = "metrics.enabled")
    Optional<Boolean> metricsEnabled;

    /**
     * Enable tracing. By default this will be enabled if the tracing extension is added.
     */
    @ConfigItem(name = "tracing.enabled")
    Optional<Boolean> tracingEnabled;

    /**
     * Enable validation. By default this will be enabled if the Hibernate Validator extension is added.
     */
    @ConfigItem(name = "validation.enabled")
    Optional<Boolean> validationEnabled;

    /**
     * Enable eventing. Allow you to receive events on bootstrap and execution.
     */
    @ConfigItem(name = "events.enabled", defaultValue = "false")
    boolean eventsEnabled;

    /**
     * Change the type naming strategy.
     */
    @ConfigItem(defaultValue = "Default")
    TypeAutoNameStrategy autoNameStrategy;

    /**
     * SmallRye GraphQL UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    SmallRyeGraphQLUIConfig ui;

}
