package io.quarkus.smallrye.graphql.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Build item for modifying the final index used by SmallRye GraphQL.
 * Extensions can produce this build item to apply custom modifications to the index before the GraphQL schema is built.
 * Modifiers are applied in priority order (lower priority values are applied first).
 * If multiple modifiers have the same priority, they are applied in the order they were produced (stable sort).
 */
public final class SmallRyeGraphQLFinalIndexModifierBuildItem extends MultiBuildItem {

    private final int priority;
    private final SmallRyeGraphQLFinalIndexModifier modifier;

    public SmallRyeGraphQLFinalIndexModifierBuildItem(int priority, SmallRyeGraphQLFinalIndexModifier modifier) {
        this.priority = priority;
        this.modifier = Assert.checkNotNullParam("modifier", modifier);
    }

    public int priority() {
        return priority;
    }

    public SmallRyeGraphQLFinalIndexModifier modifier() {
        return modifier;
    }
}
