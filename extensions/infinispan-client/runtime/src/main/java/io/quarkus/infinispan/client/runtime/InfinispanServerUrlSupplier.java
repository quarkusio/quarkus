package io.quarkus.infinispan.client.runtime;

import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ServerConfiguration;

import io.quarkus.arc.Arc;

public class InfinispanServerUrlSupplier implements Supplier<String> {

    @Override
    public String get() {
        RemoteCacheManager cacheManager = cacheManager();
        if (cacheManager == null || cacheManager.getConfiguration().servers().isEmpty()) {
            return "";
        }
        ServerConfiguration firstServer = cacheManager.getConfiguration().servers().get(0);

        return firstServer.host() + ":" + firstServer.port();
    }

    public static RemoteCacheManager cacheManager() {
        return Arc.container().instance(RemoteCacheManager.class).get();
    }
}
