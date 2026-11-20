package io.quarkus.redis.lettuce.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Request the creation of a Lettuce Redis client with the given name.
 * <p>
 * Unlike {@link io.quarkus.redis.deployment.client.RequestedRedisClientBuildItem}, this item is only produced when
 * Lettuce-specific injection points are detected (e.g. {@code RedisClient},
 * {@code StatefulRedisConnection}, {@code ClientResources} from Lettuce).
 */
public final class RequestedLettuceClientBuildItem extends MultiBuildItem {

    public final String name;
    /**
     * Whether a Lettuce connection type is injected for the client: only then are the connection beans registered,
     * and their connection opened at startup.
     */
    public final boolean connection;

    public RequestedLettuceClientBuildItem(String name, boolean connection) {
        this.name = name;
        this.connection = connection;
    }
}
