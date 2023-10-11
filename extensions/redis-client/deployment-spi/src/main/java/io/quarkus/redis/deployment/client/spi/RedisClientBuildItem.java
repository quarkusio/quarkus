package io.quarkus.redis.deployment.client.spi;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.mutiny.redis.client.Redis;

/**
 * Provides runtime access to the Redis clients, in the Mutiny variant.
 */
public final class RedisClientBuildItem extends MultiBuildItem {
    private final Supplier<Redis> client;
    private final String name;

    public RedisClientBuildItem(Supplier<Redis> client, String name) {
        this.client = client;
        this.name = name;
    }

    public Supplier<Redis> getClient() {
        return client;
    }

    public String getName() {
        return name;
    }
}
