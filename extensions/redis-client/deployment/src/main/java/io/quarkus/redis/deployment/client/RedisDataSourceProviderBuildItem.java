package io.quarkus.redis.deployment.client;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Signals that another extension provides the {@link io.quarkus.redis.datasource.RedisDataSource} and
 * {@link io.quarkus.redis.datasource.ReactiveRedisDataSource} services and beans for the requested Redis clients, so
 * the Vert.x-based implementation must not register its own.
 * <p>
 * Produced by an alternative Redis backend extension (for example {@code quarkus-redis-client-lettuce}) and
 * consumed by {@link RedisDatasourceProcessor}. As a {@link SimpleBuildItem} it can only be produced once per
 * build, so two alternative backends on the classpath fail the build instead of registering ambiguous beans.
 * <p>
 * The codecs service is still registered. The Vert.x Redis client services and beans
 * ({@code io.vertx.mutiny.redis.client.Redis}, {@code RedisAPI}, ...) are only registered for the clients that need
 * them: the ones injected through a Vert.x Redis client type, and the ones requested by another extension with a
 * {@link RequestedRedisClientBuildItem} that is not limited to the data sources (such as {@code quarkus-redis-cache}).
 * A client used only through its data sources gets no Vert.x Redis client, and the health check pings it through its
 * data source.
 */
public final class RedisDataSourceProviderBuildItem extends SimpleBuildItem {

    private final String name;

    /**
     * @param name the name of the backend providing the data source beans, used in diagnostics
     */
    public RedisDataSourceProviderBuildItem(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
