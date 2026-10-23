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
 * The codecs service and the Vert.x Redis client services and beans ({@code io.vertx.mutiny.redis.client.Redis},
 * {@code RedisAPI}, ...) are still registered for the requested clients, so the features relying on them (health
 * check, {@code quarkus-redis-cache}) keep working with the alternative backend.
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
