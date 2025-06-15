package io.quarkus.infinispan.client.deployment;

import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Provide the Infinispan clients as RuntimeValue's.
 */
public final class InfinispanClientBuildItem extends MultiBuildItem {
    private final RuntimeValue<RemoteCacheManager> client;
    private final String name;

    public InfinispanClientBuildItem(RuntimeValue<RemoteCacheManager> client, String name) {
        this.client = client;
        this.name = name;
    }

    public RuntimeValue<RemoteCacheManager> getClient() {
        return client;
    }

    public String getName() {
        return name;
    }
}
