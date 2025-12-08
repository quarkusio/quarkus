package io.quarkus.smallrye.graphql.runtime;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.graphql.spi.config.LogPayloadOption;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.smallrye-graphql")
public interface SmallRyeGraphQLRuntimeConfig {

    /**
     * If GraphQL UI should be enabled. By default, GraphQL UI is enabled if it is included (see {@code always-include}).
     */
    @WithName("ui.enabled")
    @WithDefault("true")
    boolean enabled();

    /**
     * If GraphQL UI should be enabled. By default, GraphQL UI is enabled if it is included (see {@code always-include}).
     *
     * @deprecated use {@code quarkus.smallrye-graphql.ui.enabled} instead
     */
    @WithName("ui.enable")
    @Deprecated(since = "3.26", forRemoval = true)
    Optional<Boolean> enable();

    /**
     * Specifies the field visibility for the GraphQL schema.
     * This configuration item allows you to define comma-separated list of patterns (GraphQLType.GraphQLField).
     * These patterns are used to determine which fields should be excluded from the schema.
     * Special value {@code no-introspection} will disable introspection fields.
     * For more info see <a href="https://smallrye.io/smallrye-graphql/docs/schema/field-visibility">graphql-java
     * documentation</a>
     */
    @WithDefault("default")
    String fieldVisibility();

    /**
     * Excludes all the 'null' fields in the GraphQL response's <code>data</code> field,
     * except for the non-successfully resolved fields (errors).
     * Disabled by default.
     */
    Optional<Boolean> excludeNullFieldsInResponses();

    /**
     * Exceptions that should be unwrapped (class names).
     */
    Optional<List<String>> unwrapExceptions();

    /**
     * List of Runtime Exceptions class names that should show the error message.
     * By default, Runtime Exception messages will be hidden and a generic `Server Error` message will be returned.
     */
    Optional<List<String>> showRuntimeExceptionMessage();

    /**
     * List of Checked Exceptions class names that should hide the error message.
     * By default, Checked Exception messages will show the exception message.
     */
    Optional<List<String>> hideCheckedExceptionMessage();

    /**
     * The default error message that will be used for hidden exception messages.
     * Defaults to "Server Error"
     */
    Optional<String> defaultErrorMessage();

    /**
     * List of extension fields that should be included in the error response.
     * By default, none will be included. Examples of valid values include
     * [exception,classification,code,description,validationErrorType,queryPath]
     */
    Optional<List<String>> errorExtensionFields();

    /**
     * Enable GET Requests. Allow queries via HTTP GET.
     */
    @WithName("http.get.enabled")
    @WithDefault("false")
    boolean httpGetEnabled();

    /**
     * Enable Query parameter on POST Requests. Allow POST request to override or supply values in a query parameter.
     */
    @WithName("http.post.queryparameters.enabled")
    @WithDefault("false")
    boolean httpPostQueryParametersEnabled();

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
}
