package io.quarkus.smallrye.graphql.deployment;

import org.jboss.jandex.IndexView;

/**
 * Interface for modifying the final index used by SmallRye GraphQL.
 * Implementations can wrap, enhance, or filter the index before it's used to build the GraphQL schema.
 */
public interface SmallRyeGraphQLFinalIndexModifier {

    /**
     * Modifies the given index.
     *
     * @param index the current index
     * @return the modified index (must not be null)
     */
    IndexView modify(IndexView index);
}
