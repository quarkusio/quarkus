package io.quarkus.smallrye.graphql.runtime;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.graphql.spi.config.LogPayloadOption;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.smallrye-graphql")
public interface SmallRyeGraphQLConfig {

    /**
     * The rootPath under which queries will be served. Default to graphql
     * By default, this value will be resolved as a path relative to `${quarkus.http.root-path}`.
     */
    @WithDefault("graphql")
    String rootPath();

    /**
     * Enable Apollo Federation. If this value is unspecified, then federation will be enabled
     * automatically if any GraphQL Federation annotations are detected in the application.
     */
    @WithName("federation.enabled")
    Optional<Boolean> federationEnabled();

    /**
     * Enable batch resolving for federation. Disabled by default.
     */
    @WithName("federation.batch-resolving-enabled")
    Optional<Boolean> federationBatchResolvingEnabled();

    /**
     * Enable metrics. By default, this is false. If set to true, a metrics extension is required.
     */
    @WithName("metrics.enabled")
    Optional<Boolean> metricsEnabled();

    /**
     * Enable tracing. By default, this will be enabled if the tracing extension is added.
     */
    @WithName("tracing.enabled")
    Optional<Boolean> tracingEnabled();

    /**
     * Enable eventing. Allow you to receive events on bootstrap and execution.
     */
    @WithName("events.enabled")
    @WithDefault("false")
    boolean eventsEnabled();

    /**
     * Enable non-blocking support. Default is true.
     */
    @WithName("nonblocking.enabled")
    Optional<Boolean> nonBlockingEnabled();

    /**
     * Enable GET Requests. Allow queries via HTTP GET.
     */
    @WithName("http.get.enabled")
    Optional<Boolean> httpGetEnabled();

    /**
     * Enable Query parameter on POST Requests. Allow POST request to override or supply values in a query parameter.
     */
    @WithName("http.post.queryparameters.enabled")
    Optional<Boolean> httpPostQueryParametersEnabled();

    /**
     * Change the type naming strategy.
     * All possible strategies are: default, merge-inner-class, full
     */
    @WithDefault("Default")
    String autoNameStrategy();

    /**
     * List of extension fields that should be included in the error response.
     * By default, none will be included. Examples of valid values include
     * [exception,classification,code,description,validationErrorType,queryPath]
     */
    Optional<List<String>> errorExtensionFields();

    /**
     * The default error message that will be used for hidden exception messages.
     * Defaults to "Server Error"
     */
    Optional<String> defaultErrorMessage();

    /**
     * Print the data fetcher exception to the log file. Default `true` in dev and test mode, default `false` in prod.
     */
    Optional<Boolean> printDataFetcherException();

    /**
     * Make the schema available over HTTP.
     */
    @WithDefault("true")
    boolean schemaAvailable();

    /**
     * Include the Scalar definitions in the schema.
     */
    @WithDefault("false")
    boolean schemaIncludeScalars();

    /**
     * Include the schema internal definition in the schema.
     */
    @WithDefault("false")
    boolean schemaIncludeSchemaDefinition();

    /**
     * Include Directives in the schema.
     */
    @WithDefault("false")
    boolean schemaIncludeDirectives();

    /**
     * Include Introspection Types in the schema.
     */
    @WithDefault("false")
    boolean schemaIncludeIntrospectionTypes();

    /**
     * Log the payload (and optionally variables) to System out.
     */
    @WithDefault("off")
    LogPayloadOption logPayload();

    /**
     * Exceptions that should be unwrapped (class names).
     */
    Optional<List<String>> unwrapExceptions();

    /**
     * Subprotocols that should be supported by the server for graphql-over-websocket use cases.
     * Allowed subprotocols are "graphql-ws" and "graphql-transport-ws". By default, both are enabled.
     */
    Optional<List<String>> websocketSubprotocols();

    /**
     * Set to true if ignored chars should be captured as AST nodes. Default to false
     */
    Optional<Boolean> parserCaptureIgnoredChars();

    /**
     * Set to true if `graphql.language.Comment`s should be captured as AST nodes
     */
    Optional<Boolean> parserCaptureLineComments();

    /**
     * Set to true true if `graphql.language.SourceLocation`s should be captured as AST nodes. Default to true
     */
    Optional<Boolean> parserCaptureSourceLocation();

    /**
     * The maximum number of raw tokens the parser will accept, after which an exception will be thrown. Default to 15000
     */
    OptionalInt parserMaxTokens();

    /**
     * The maximum number of raw whitespace tokens the parser will accept, after which an exception will be thrown. Default to
     * 200000
     */
    OptionalInt parserMaxWhitespaceTokens();

    /**
     * Abort a query if the total number of data fields queried exceeds the defined limit. Default to no limit
     */
    OptionalInt instrumentationQueryComplexity();

    /**
     * Abort a query if the total depth of the query exceeds the defined limit. Default to no limit
     */
    OptionalInt instrumentationQueryDepth();

    /**
     * SmallRye GraphQL UI configuration
     */
    @ConfigDocSection
    SmallRyeGraphQLUIConfig ui();

    /**
     * Additional scalars to register in the schema.
     * These are taken from the `graphql-java-extended-scalars` library.
     */
    Optional<List<ExtraScalar>> extraScalars();
}
