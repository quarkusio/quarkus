package io.quarkus.reactive.datasource.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.sqlclient.Pool;

/**
 * A build item for Vertx {@link Pool}s.
 * <p>
 * If you inject this build item when recording runtime init template calls, you are guaranteed the Pool configuration
 * has been injected and Pools can be created.
 */
@Deprecated(forRemoval = true)
public final class VertxPoolBuildItem extends MultiBuildItem {

    public VertxPoolBuildItem() {
    }

    public VertxPoolBuildItem(RuntimeValue<? extends Pool> vertxPool, String dbKind, boolean isDefault) {

    }

    public RuntimeValue<? extends Pool> getPool() {
        throw new IllegalStateException("should never be called");
    }

    public String getDbKind() {
        throw new IllegalStateException("should never be called");
    }

    public boolean isDefault() {
        throw new IllegalStateException("should never be called");
    }

}
