package io.quarkus.smallrye.graphql.client.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Declares that the extension producing this build item provides the HTTP {@code Authorization} header
 * for the GraphQL client with the given configuration key.
 * <p>
 * Extensions which provide the {@code Authorization} header for GraphQL clients that were not explicitly selected,
 * for example for all the GraphQL clients, must not provide it for this GraphQL client.
 */
public final class GraphQLClientAuthorizationHeaderBuildItem extends MultiBuildItem {

    private final String configKey;
    private final String provider;

    /**
     * @param configKey configuration key of the GraphQL client
     * @param provider description of what provides the {@code Authorization} header, it is used in the error messages
     */
    public GraphQLClientAuthorizationHeaderBuildItem(String configKey, String provider) {
        this.configKey = Objects.requireNonNull(configKey);
        this.provider = Objects.requireNonNull(provider);
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getProvider() {
        return provider;
    }
}
