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
     * Enable Apollo Federation. If this value is unspecified, then federation will be enabled
     * automatically if any GraphQL Federation annotations are detected in the application.
     */
    @ConfigItem(name = "federation.enabled")
    public Optional<Boolean> federationEnabled;

    /**
     * Enable batch resolving for federation. Disabled by default.
     */
    @ConfigItem(name = "federation.batch-resolving-enabled")
    public Optional<Boolean> federationBatchResolvingEnabled;

    /**
     * Enable metrics. By default, this is false. If set to true, a metrics extension is required.
     */
    @ConfigItem(name = "metrics.enabled")
    public Optional<Boolean> metricsEnabled;

    /**
     * Enable tracing. By default, this will be enabled if the tracing extension is added.
     */
    @ConfigItem(name = "tracing.enabled")
    public Optional<Boolean> tracingEnabled;

    /**
     * Enable eventing. Allow you to receive events on bootstrap and execution.
     */
    @ConfigItem(name = "events.enabled", defaultValue = "false")
    public boolean eventsEnabled;

    /**
     * Enable non-blocking support. Default is true.
     */
    @ConfigItem(name = "nonblocking.enabled")
    public Optional<Boolean> nonBlockingEnabled;

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
     * By default, none will be included. Examples of valid values include
     * [exception,classification,code,description,validationErrorType,queryPath]
     */
    @ConfigItem
    public Optional<List<String>> errorExtensionFields;

    /**
     * List of Runtime Exceptions class names that should show the error message.
     * By default, Runtime Exception messages will be hidden and a generic `Server Error` message will be returned.
     */
    @ConfigItem
    public Optional<List<String>> showRuntimeExceptionMessage;

    /**
     * List of Checked Exceptions class names that should hide the error message.
     * By default, Checked Exception messages will show the exception message.
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
     * Subprotocols that should be supported by the server for graphql-over-websocket use cases.
     * Allowed subprotocols are "graphql-ws" and "graphql-transport-ws". By default, both are enabled.
     */
    @ConfigItem
    public Optional<List<String>> websocketSubprotocols;

    /**
     * Set to true if ignored chars should be captured as AST nodes. Default to false
     */
    @ConfigItem
    public Optional<Boolean> parserCaptureIgnoredChars;

    /**
     * Set to true if `graphql.language.Comment`s should be captured as AST nodes
     */
    @ConfigItem
    public Optional<Boolean> parserCaptureLineComments;

    /**
     * Set to true true if `graphql.language.SourceLocation`s should be captured as AST nodes. Default to true
     */
    @ConfigItem
    public Optional<Boolean> parserCaptureSourceLocation;

    /**
     * The maximum number of raw tokens the parser will accept, after which an exception will be thrown. Default to 15000
     */
    @ConfigItem
    public Optional<Integer> parserMaxTokens;

    /**
     * The maximum number of raw whitespace tokens the parser will accept, after which an exception will be thrown. Default to
     * 200000
     */
    @ConfigItem
    public Optional<Integer> parserMaxWhitespaceTokens;

    /**
     * Abort a query if the total number of data fields queried exceeds the defined limit. Default to no limit
     */
    @ConfigItem
    public Optional<Integer> instrumentationQueryComplexity;

    /**
     * Abort a query if the total depth of the query exceeds the defined limit. Default to no limit
     */
    @ConfigItem
    public Optional<Integer> instrumentationQueryDepth;

    /**
     * SmallRye GraphQL UI configuration
     */
    @ConfigItem
    @ConfigDocSection
    public SmallRyeGraphQLUIConfig ui;
}
