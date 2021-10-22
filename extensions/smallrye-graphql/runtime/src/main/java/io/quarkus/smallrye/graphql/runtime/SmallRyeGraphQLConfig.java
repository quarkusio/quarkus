package io.quarkus.smallrye.graphql.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.graphql.schema.helper.TypeAutoNameStrategy;
import io.smallrye.graphql.spi.config.LogPayloadOption;

@ConfigRoot(name = "smallrye-graphql", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SmallRyeGraphQLConfig {

    /**
     * The rootPath under which queries will be served. Default to graphql
     * By default, this value will be resolved as a path relative to `${quarkus.http.root-path}`.
     */
    @ConfigItem(defaultValue = "graphql")
    public String rootPath;

    /**
     * Enable metrics. By default this is false. If set to true, a metrics extension is required.
     */
    @ConfigItem(name = "metrics.enabled")
    public Optional<Boolean> metricsEnabled;

    /**
     * Enable tracing. By default this will be enabled if the tracing extension is added.
     */
    @ConfigItem(name = "tracing.enabled")
    public Optional<Boolean> tracingEnabled;

    /**
     * Enable validation. By default this will be enabled if the Hibernate Validator extension is added.
     */
    @ConfigItem(name = "validation.enabled")
    public Optional<Boolean> validationEnabled;

    /**
     * Enable eventing. Allow you to receive events on bootstrap and execution.
     */
    @ConfigItem(name = "events.enabled", defaultValue = "false")
    public boolean eventsEnabled;

    /**
     * Enable GET Requests. Allow queries via HTTP GET.
     */
    @ConfigItem(name = "http.get.enabled")
    public Optional<Boolean> httpGetEnabled;

    /**
     * Enable Query parameter on POST Requests. Allow POST request to override or supply values in a query parameter.
     */
    @ConfigItem(name = "http.post.queryparameters.enabled")
    public Optional<Boolean> httpPostQueryParametersEnabled;

    /**
     * Change the type naming strategy.
     */
    @ConfigItem(defaultValue = "Default")
    public TypeAutoNameStrategy autoNameStrategy;

    /**
     * List of extension fields that should be included in the error response.
     * By default none will be included. Examples of valid values include
     * [exception,classification,code,description,validationErrorType,queryPath]
     */
    @ConfigItem
    public Optional<List<String>> errorExtensionFields;

    /**
     * List of Runtime Exceptions class names that should show the error message.
     * By default Runtime Exception messages will be hidden and a generic `Server Error` message will be returned.
     */
    @ConfigItem
    public Optional<List<String>> showRuntimeExceptionMessage;

    /**
     * List of Checked Exceptions class names that should hide the error message.
     * By default Checked Exception messages will show the exception message.
     */
    @ConfigItem
    public Optional<List<String>> hideCheckedExceptionMessage;

    /**
     * The default error message that will be used for hidden exception messages.
     * Defaults to "Server Error"
     */
    @ConfigItem
    public Optional<String> defaultErrorMessage;

    /**
     * Print the data fetcher exception to the log file. Default `true` in dev and test mode, default `false` in prod.
     */
    @ConfigItem
    public Optional<Boolean> printDataFetcherException;

    /**
     * Make the schema available over HTTP.
     */
    @ConfigItem(defaultValue = "true")
    public boolean schemaAvailable;

    /**
     * Include the Scalar definitions in the schema.
     */
    @ConfigItem(defaultValue = "false")
    public boolean schemaIncludeScalars;

    /**
     * Include the schema internal definition in the schema.
     */
    @ConfigItem(defaultValue = "false")
    public boolean schemaIncludeSchemaDefinition;

    /**
     * Include Directives in the schema.
     */
    @ConfigItem(defaultValue = "false")
    public boolean schemaIncludeDirectives;

    /**
     * Include Introspection Types in the schema.
     */
    @ConfigItem(defaultValue = "false")
    public boolean schemaIncludeIntrospectionTypes;

    /**
     * Log the payload (and optionally variables) to System out.
     */
    @ConfigItem(defaultValue = "off")
    public LogPayloadOption logPayload;

    /**
     * Set the Field visibility.
     */
    @ConfigItem(defaultValue = "default")
    public String fieldVisibility;

    /**
     * Exceptions that should be unwrapped (class names).
     */
    @ConfigItem
    public Optional<List<String>> unwrapExceptions;

    /**
     * SmallRye GraphQL UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    public SmallRyeGraphQLUIConfig ui;
}
