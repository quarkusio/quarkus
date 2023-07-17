package io.quarkus.redis.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Request the creation of the Redis client with the given name.
 */
public final class RequestedRedisClientBuildItem extends MultiBuildItem {

    public final String name;

    public RequestedRedisClientBuildItem(String name) {
        this.name = name;
    }

}
