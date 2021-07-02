package io.quarkus.infinispan.client.runtime;

import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.arc.Arc;

public class InfinispanClientSupplier implements Supplier<RemoteCacheManager> {

    @Override
    public RemoteCacheManager get() {
        RemoteCacheManager cacheManager = cacheManager();
        return cacheManager;
    }

    public static RemoteCacheManager cacheManager() {
        return Arc.container().instance(RemoteCacheManager.class).get();
    }
}
