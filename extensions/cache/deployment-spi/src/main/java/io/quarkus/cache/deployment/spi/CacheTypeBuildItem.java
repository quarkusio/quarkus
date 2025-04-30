package io.quarkus.cache.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that can be used by extensions to determine what kind of cache backend is configured.
 * This is useful for cases where caching extensions specific data does not make sense for remote cache backends
 */
public final class CacheTypeBuildItem extends SimpleBuildItem {

    private final Type type;

    public CacheTypeBuildItem(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        LOCAL,
        REMOTE
    }
}
