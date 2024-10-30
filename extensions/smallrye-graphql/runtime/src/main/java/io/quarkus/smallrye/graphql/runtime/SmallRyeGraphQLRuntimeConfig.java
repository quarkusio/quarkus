package io.quarkus.smallrye.graphql.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-graphql", phase = ConfigPhase.RUN_TIME)
public class SmallRyeGraphQLRuntimeConfig {

    /**
     * If GraphQL UI should be enabled. By default, GraphQL UI is enabled if it is included (see {@code always-include}).
     */
    @ConfigItem(name = "ui.enable", defaultValue = "true")
    boolean enable;

    /**
     * Specifies the field visibility for the GraphQL schema.
     * This configuration item allows you to define comma-separated list of patterns (GraphQLType.GraphQLField).
     * These patterns are used to determine which fields should be excluded from the schema.
     * Special value {@code no-introspection} will disable introspection fields.
     * For more info see <a href="https://smallrye.io/smallrye-graphql/docs/schema/field-visibility">graphql-java
     * documentation</a>
     */
    @ConfigItem(defaultValue = "default")
    public String fieldVisibility;
}
