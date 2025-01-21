package io.quarkus.smallrye.graphql.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.smallrye-graphql")
public interface SmallRyeGraphQLRuntimeConfig {

    /**
     * If GraphQL UI should be enabled. By default, GraphQL UI is enabled if it is included (see {@code always-include}).
     */
    @WithName("ui.enable")
    @WithDefault("true")
    boolean enable();

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
     * List of Runtime Exceptions class names that should show the error message.
     * By default, Runtime Exception messages will be hidden and a generic `Server Error` message will be returned.
     */
    Optional<List<String>> showRuntimeExceptionMessage();

    /**
     * List of Checked Exceptions class names that should hide the error message.
     * By default, Checked Exception messages will show the exception message.
     */
    Optional<List<String>> hideCheckedExceptionMessage();
}
