package io.quarkus.smallrye.graphql.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.graphql.schema.helper.TypeAutoNameStrategy;

@ConfigRoot(name = "smallrye-graphql")
public class SmallRyeGraphQLConfig {

    /**
     * The rootPath under which queries will be served. Default to graphql
     * By default, this value will be resolved as a path relative to `${quarkus.http.root-path}`.
     */
    @ConfigItem(defaultValue = "graphql")
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
     * Enable GET Requests. Allow queries via HTTP GET.
     */
    @ConfigItem(name = "http.get.enabled")
    Optional<Boolean> httpGetEnabled;

    /**
     * Enable Query parameter on POST Requests. Allow POST request to override or supply values in a query parameter.
     */
    @ConfigItem(name = "http.post.queryparameters.enabled")
    Optional<Boolean> httpPostQueryParametersEnabled;

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
