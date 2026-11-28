package io.quarkus.oidc.token.propagation.graphql.deployment.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a GraphQL client which propagates the access token as the HTTP {@code Authorization} header.
 * <p>
 * Produced by the {@code quarkus-oidc-token-propagation-graphql} extension, once for each configuration key of the
 * GraphQL clients annotated with the {@code @AccessToken} annotation.
 * Consumed by the extensions which would otherwise provide the {@code Authorization} header of this GraphQL client.
 */
public final class GraphQLClientTokenPropagationBuildItem extends MultiBuildItem {

    private final String configKey;

    /**
     * @param configKey configuration key of the GraphQL client
     */
    public GraphQLClientTokenPropagationBuildItem(String configKey) {
        this.configKey = Objects.requireNonNull(configKey);
    }

    public String getConfigKey() {
        return configKey;
    }
}
