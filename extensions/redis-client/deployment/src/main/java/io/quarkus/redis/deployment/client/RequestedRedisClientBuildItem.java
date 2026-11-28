package io.quarkus.redis.deployment.client;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Request the creation of the Redis client with the given name.
 */
public final class RequestedRedisClientBuildItem extends MultiBuildItem {

    public final String name;

    /**
     * Whether the client is only used through its data sources ({@code RedisDataSource} and
     * {@code ReactiveRedisDataSource}). When another backend provides the data sources (see
     * {@link RedisDataSourceProviderBuildItem}), the Vert.x Redis client is not created for such a request.
     */
    public final boolean dataSourceOnly;

    public RequestedRedisClientBuildItem(String name) {
        this(name, false);
    }

    private RequestedRedisClientBuildItem(String name, boolean dataSourceOnly) {
        this.name = name;
        this.dataSourceOnly = dataSourceOnly;
    }

    /**
     * Request the creation of the data sources of the Redis client with the given name, for a client that is not used
     * through the Vert.x Redis client types.
     *
     * @param name the client name
     * @return the request
     */
    public static RequestedRedisClientBuildItem dataSource(String name) {
        return new RequestedRedisClientBuildItem(name, true);
    }

}
