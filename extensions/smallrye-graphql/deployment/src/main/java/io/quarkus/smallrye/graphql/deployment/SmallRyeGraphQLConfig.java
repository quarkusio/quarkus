package io.quarkus.smallrye.graphql.deployment;

import java.util.List;
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
     * Enable metrics. By default this is false. If set to true, a metrics extension is required.
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
     * List of extension fields that should be included in the error response.
     * By default none will be included. Examples of valid values include
     * [exception,classification,code,description,validationErrorType,queryPath]
     */
    @ConfigItem
    Optional<List<String>> errorExtensionFields;

    /**
     * List of Runtime Exceptions class names that should show the error message.
     * By default Runtime Exception messages will be hidden and a generic `Server Error` message will be returned.
     */
    @ConfigItem
    Optional<List<String>> showRuntimeExceptionMessage;

    /**
     * List of Checked Exceptions class names that should hide the error message.
     * By default Checked Exception messages will show the exception message.
     */
    @ConfigItem
    Optional<List<String>> hideCheckedExceptionMessage;

    /**
     * The default error message that will be used for hidden exception messages.
     * Defaults to "Server Error"
     */
    @ConfigItem
    Optional<String> defaultErrorMessage;

    /**
     * SmallRye GraphQL UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    SmallRyeGraphQLUIConfig ui;

}
